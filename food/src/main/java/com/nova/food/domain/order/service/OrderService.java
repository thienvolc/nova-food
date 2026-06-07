package com.nova.food.domain.order.service;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.common.dto.PageResponse;
import com.nova.food.domain.common.service.IdempotencyKeyService;
import com.nova.food.domain.common.service.IdempotencyLockManager;
import com.nova.food.domain.common.service.PageValidator;
import com.nova.food.domain.events.outbox.OutboxService;
import com.nova.food.domain.menu.entity.MenuItemEntity;
import com.nova.food.domain.menu.policy.InventoryPolicyService;
import com.nova.food.domain.menu.service.MenuItemService;
import com.nova.food.domain.order.constant.OrderStatus;
import com.nova.food.domain.order.dto.event.OrderCreatedEvent;
import com.nova.food.domain.order.dto.event.OrderReadyForDeliveryEvent;
import com.nova.food.domain.order.dto.request.CancelOrderRequest;
import com.nova.food.domain.order.dto.request.CreateOrderRequest;
import com.nova.food.domain.order.dto.response.OrderResponse;
import com.nova.food.domain.order.dto.response.OrderStatusHistoryResponse;
import com.nova.food.domain.order.dto.response.OrderTrackingResponse;
import com.nova.food.domain.order.entity.OrderEntity;
import com.nova.food.domain.order.entity.OrderItemEntity;
import com.nova.food.domain.order.entity.OrderStatusHistoryEntity;
import com.nova.food.domain.order.mapper.OrderMapper;
import com.nova.food.domain.order.policy.OrderPolicyService;
import com.nova.food.domain.order.repository.OrderItemRepository;
import com.nova.food.domain.order.repository.OrderRepository;
import com.nova.food.domain.order.repository.OrderStatusHistoryRepository;
import com.nova.food.domain.restaurant.entity.RestaurantEntity;
import com.nova.food.domain.restaurant.service.RestaurantService;
import com.nova.food.infrastructure.kafka.producer.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final MenuItemService menuItemService;
    private final InventoryPolicyService inventoryPolicyService;
    private final RestaurantService restaurantService;
    private final OrderMapper orderMapper;
    private final OrderPolicyService orderPolicyService;
    private final IdempotencyLockManager idempotencyLockManager;
    private final IdempotencyKeyService idempotencyKeyService;
    private final OrderRequestFingerprintService orderRequestFingerprintService;
    private final OrderCreateIdempotencyService orderCreateIdempotencyService;
    private final OutboxService outboxService;
    private final DomainEventPublisher domainEventPublisher;
    private final PageValidator pageValidator;

    @Transactional
    public OrderResponse create(CreateOrderRequest request, UUID customerId) {
        return create(request, customerId, null);
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, UUID customerId, String idempotencyKey) {
        String normalizedKey = idempotencyKeyService.normalize(idempotencyKey);
        if (normalizedKey == null) {
            return createInternal(request, customerId, null, null);
        }
        String requestFingerprint = orderRequestFingerprintService.fingerprint(request);
        String lockKey = "order-create:" + customerId + ":" + normalizedKey;
        return idempotencyLockManager.executeWithLock(
                lockKey,
                () -> createInternal(request, customerId, normalizedKey, requestFingerprint)
        );
    }

    private OrderResponse createInternal(CreateOrderRequest request,
                                         UUID customerId,
                                         String normalizedKey,
                                         String requestFingerprint) {
        OrderEntity existingOrder = orderCreateIdempotencyService.findExisting(customerId, normalizedKey)
                .map(order -> orderCreateIdempotencyService.requireSameRequest(order, requestFingerprint))
                .orElse(null);
        if (existingOrder != null) {
            return toResponse(existingOrder);
        }

        OrderDraft orderDraft = prepareOrderDraft(request, customerId, normalizedKey, requestFingerprint);
        OrderEntity savedOrder;
        try {
            savedOrder = orderRepository.save(orderDraft.order());
        } catch (DataIntegrityViolationException exception) {
            OrderEntity recoveredOrder = orderCreateIdempotencyService.recoverFromUniqueViolation(
                            customerId,
                            normalizedKey,
                            exception
                    )
                    .map(order -> orderCreateIdempotencyService.requireSameRequest(order, requestFingerprint))
                    .orElseThrow(() -> exception);
            return toResponse(recoveredOrder);
        }
        List<OrderItemEntity> savedItems = orderItemRepository.saveAll(orderDraft.items());
        recordStatusHistory(savedOrder.getId(), null, savedOrder.getStatus(), customerId, "Order created");
        publishOrderCreated(savedOrder);
        return orderMapper.toResponse(savedOrder, savedItems);
    }

    private OrderDraft prepareOrderDraft(CreateOrderRequest request,
                                         UUID customerId,
                                         String normalizedKey,
                                         String requestFingerprint) {
        orderPolicyService.validateCreateItems(request.items());
        Map<UUID, MenuItemEntity> menuItems = menuItemService.getRequiredMenuItems(menuItemIds(request.items()));
        UUID restaurantId = inventoryPolicyService.validateMenuItemsForOrder(menuItems, requestedQuantities(request.items()));
        RestaurantEntity restaurant = restaurantService.getRequiredRestaurant(restaurantId);
        orderPolicyService.validateRestaurantActive(restaurant);

        UUID orderId = UUID.randomUUID();
        List<OrderItemEntity> orderItems = createOrderItems(orderId, request.items(), menuItems);
        BigDecimal subtotal = orderItems.stream()
                .map(OrderItemEntity::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant now = Instant.now();
        OrderEntity order = OrderEntity.builder()
                .id(orderId)
                .customerId(customerId)
                .restaurantId(restaurantId)
                .trackingId(generateTrackingId())
                .idempotencyKey(normalizedKey)
                .requestFingerprint(requestFingerprint)
                .status(OrderStatus.PENDING_PAYMENT)
                .subtotal(subtotal)
                .total(subtotal)
                .stockDecremented(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return new OrderDraft(order, orderItems);
    }

    @Transactional(readOnly = true)
    public OrderTrackingResponse track(String trackingId, UUID userId, boolean admin, boolean restaurantOwner) {
        OrderEntity order = orderRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
        validateCanViewLifecycle(order, userId, admin, restaurantOwner);
        return orderMapper.toTrackingResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID orderId, UUID userId, boolean admin) {
        OrderEntity order = getRequiredOrder(orderId);
        validateCanView(order, userId, admin);
        return orderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listMyOrders(UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(order -> orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listMyOrders(UUID customerId, int page, int size) {
        pageValidator.validate(page, size);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = orderRepository.findByCustomerId(customerId, pageable)
                .map(order -> orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId())));
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> listStatusHistory(UUID orderId,
                                                              UUID userId,
                                                              boolean admin,
                                                              boolean restaurantOwner) {
        OrderEntity order = getRequiredOrder(orderId);
        validateCanViewLifecycle(order, userId, admin, restaurantOwner);
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId).stream()
                .map(orderMapper::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listRestaurantOrders(UUID restaurantId, UUID userId, boolean admin) {
        RestaurantEntity restaurant = restaurantService.getRequiredRestaurant(restaurantId);
        restaurantService.validateCanManage(restaurant, userId, admin);
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .map(order -> orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listRestaurantOrders(UUID restaurantId, UUID userId, boolean admin, int page, int size) {
        pageValidator.validate(page, size);
        RestaurantEntity restaurant = restaurantService.getRequiredRestaurant(restaurantId);
        restaurantService.validateCanManage(restaurant, userId, admin);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = orderRepository.findByRestaurantId(restaurantId, pageable)
                .map(order -> orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId())));
        return PageResponse.from(result);
    }

    @Transactional
    public OrderResponse confirm(UUID orderId, UUID userId, boolean admin) {
        OrderEntity order = getRestaurantOrderForMutation(orderId, userId, admin);
        orderPolicyService.requireStatus(order, OrderStatus.PAID);
        OrderStatus fromStatus = order.getStatus();
        order.markConfirmed();
        recordStatusHistory(order.getId(), fromStatus, order.getStatus(), userId, null);
        return orderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional
    public OrderResponse markPreparing(UUID orderId, UUID userId, boolean admin) {
        OrderEntity order = getRestaurantOrderForMutation(orderId, userId, admin);
        orderPolicyService.requireStatus(order, OrderStatus.CONFIRMED);
        OrderStatus fromStatus = order.getStatus();
        order.markPreparing();
        recordStatusHistory(order.getId(), fromStatus, order.getStatus(), userId, null);
        return orderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional
    public OrderResponse markReadyForDelivery(UUID orderId, UUID userId, boolean admin) {
        OrderEntity order = getRestaurantOrderForMutation(orderId, userId, admin);
        orderPolicyService.requireStatus(order, OrderStatus.PREPARING);
        OrderStatus fromStatus = order.getStatus();
        order.markReadyForDelivery();
        recordStatusHistory(order.getId(), fromStatus, order.getStatus(), userId, null);
        outboxService.append(new OrderReadyForDeliveryEvent(
                order.getId(),
                order.getRestaurantId(),
                userId,
                Instant.now()
        ));
        return orderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional
    public OrderResponse cancelByCustomer(UUID orderId, CancelOrderRequest request, UUID customerId) {
        OrderEntity order = getRequiredOrder(orderId);
        validateOwnsOrder(order, customerId);
        orderPolicyService.validateCustomerCanCancel(order);
        cancel(order, customerId, request.reason());
        return orderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional
    public OrderResponse cancelByRestaurant(UUID orderId, CancelOrderRequest request, UUID userId, boolean admin) {
        OrderEntity order = getRestaurantOrderForMutation(orderId, userId, admin);
        orderPolicyService.validateRestaurantCanCancel(order);
        cancel(order, userId, request.reason());
        return orderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional(readOnly = true)
    public OrderEntity getRequiredOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }

    public void validateOwnsOrder(OrderEntity order, UUID customerId) {
        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessException(ResponseCode.ACCESS_DENIED);
        }
    }

    public void validateOrderStatus(OrderEntity order, OrderStatus status) {
        orderPolicyService.requireStatus(order, status);
    }

    public void markPaid(OrderEntity order, UUID changedBy) {
        OrderStatus fromStatus = order.getStatus();
        decrementStockIfNeeded(order);
        order.markPaid();
        recordStatusHistory(order.getId(), fromStatus, order.getStatus(), changedBy, null);
    }

    public void markDelivering(OrderEntity order, UUID changedBy) {
        OrderStatus fromStatus = order.getStatus();
        order.markDelivering();
        recordStatusHistory(order.getId(), fromStatus, order.getStatus(), changedBy, null);
    }

    public void markCompleted(OrderEntity order, UUID changedBy) {
        OrderStatus fromStatus = order.getStatus();
        order.markCompleted();
        recordStatusHistory(order.getId(), fromStatus, order.getStatus(), changedBy, null);
    }

    private void validateCanView(OrderEntity order, UUID userId, boolean admin) {
        if (!admin && !order.getCustomerId().equals(userId)) {
            throw new BusinessException(ResponseCode.ACCESS_DENIED);
        }
    }

    private void validateCanViewLifecycle(OrderEntity order, UUID userId, boolean admin, boolean restaurantOwner) {
        if (admin || order.getCustomerId().equals(userId)) {
            return;
        }
        if (restaurantOwner) {
            RestaurantEntity restaurant = restaurantService.getRequiredRestaurant(order.getRestaurantId());
            restaurantService.validateCanManage(restaurant, userId, false);
            return;
        }
        throw new BusinessException(ResponseCode.ACCESS_DENIED);
    }

    private OrderEntity getRestaurantOrderForMutation(UUID orderId, UUID userId, boolean admin) {
        OrderEntity order = getRequiredOrder(orderId);
        RestaurantEntity restaurant = restaurantService.getRequiredRestaurant(order.getRestaurantId());
        restaurantService.validateCanManage(restaurant, userId, admin);
        return order;
    }

    private void cancel(OrderEntity order, UUID changedBy, String reason) {
        OrderStatus fromStatus = order.getStatus();
        boolean refundDeferred = order.getStatus() != OrderStatus.PENDING_PAYMENT;
        restoreStockIfAllowed(order);
        order.markCancelled();
        recordStatusHistory(order.getId(), fromStatus, order.getStatus(), changedBy,
                cancellationReason(reason, refundDeferred));
    }

    private String cancellationReason(String reason, boolean refundDeferred) {
        String cleanReason = clean(reason);
        if (!refundDeferred) {
            return cleanReason;
        }
        if (cleanReason == null) {
            return "Refund deferred";
        }
        return "Refund deferred: " + cleanReason;
    }

    private void recordStatusHistory(UUID orderId,
                                     OrderStatus fromStatus,
                                     OrderStatus toStatus,
                                     UUID changedBy,
                                     String reason) {
        orderStatusHistoryRepository.save(OrderStatusHistoryEntity.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedByUserId(changedBy)
                .reason(reason)
                .changedAt(Instant.now())
                .build());
    }

    private String generateTrackingId() {
        String trackingId;
        do {
            trackingId = "NF" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
        } while (orderRepository.existsByTrackingId(trackingId));
        return trackingId;
    }

    private void publishOrderCreated(OrderEntity order) {
        domainEventPublisher.publish(new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getTotal(),
                Instant.now()
        ));
    }

    private OrderResponse toResponse(OrderEntity order) {
        return orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Set<UUID> menuItemIds(List<CreateOrderRequest.Item> items) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (CreateOrderRequest.Item item : items) {
            ids.add(item.menuItemId());
        }
        return ids;
    }

    private Map<UUID, Integer> requestedQuantities(List<CreateOrderRequest.Item> items) {
        return items.stream().collect(Collectors.toMap(
                CreateOrderRequest.Item::menuItemId,
                CreateOrderRequest.Item::quantity,
                Integer::sum
        ));
    }

    private void decrementStockIfNeeded(OrderEntity order) {
        if (order.isStockDecremented()) {
            return;
        }
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(order.getId());
        Map<UUID, MenuItemEntity> menuItems = menuItemService.getRequiredMenuItems(menuItemIdsFromOrderItems(orderItems));
        for (OrderItemEntity item : orderItems) {
            inventoryPolicyService.decrementStock(menuItems.get(item.getMenuItemId()), item.getQuantity());
        }
        order.markStockDecremented();
    }

    private void restoreStockIfAllowed(OrderEntity order) {
        if (!order.isStockDecremented() || order.getStatus() == OrderStatus.PREPARING) {
            return;
        }
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(order.getId());
        Map<UUID, MenuItemEntity> menuItems = menuItemService.getRequiredMenuItems(menuItemIdsFromOrderItems(orderItems));
        for (OrderItemEntity item : orderItems) {
            inventoryPolicyService.restoreStock(menuItems.get(item.getMenuItemId()), item.getQuantity());
        }
        order.markStockRestored();
    }

    private Set<UUID> menuItemIdsFromOrderItems(List<OrderItemEntity> orderItems) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (OrderItemEntity item : orderItems) {
            ids.add(item.getMenuItemId());
        }
        return ids;
    }

    private List<OrderItemEntity> createOrderItems(UUID orderId,
                                                   List<CreateOrderRequest.Item> requestItems,
                                                   Map<UUID, MenuItemEntity> menuItems) {
        List<OrderItemEntity> orderItems = new ArrayList<>();
        for (CreateOrderRequest.Item requestItem : requestItems) {
            MenuItemEntity menuItem = menuItems.get(requestItem.menuItemId());
            BigDecimal quantity = BigDecimal.valueOf(requestItem.quantity());
            BigDecimal lineTotal = menuItem.getPrice().multiply(quantity);
            orderItems.add(OrderItemEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .menuItemId(menuItem.getId())
                    .menuItemName(menuItem.getName())
                    .unitPrice(menuItem.getPrice())
                    .quantity(requestItem.quantity())
                    .lineTotal(lineTotal)
                    .build());
        }
        return orderItems;
    }

    private record OrderDraft(OrderEntity order, List<OrderItemEntity> items) {
    }
}
