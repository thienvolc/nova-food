package com.techora.domain.order.service;

import com.techora.app.aop.BusinessException;
import com.techora.app.dto.response.PageResponse;
import com.techora.domain.cart.entity.CartEntity;
import com.techora.domain.cart.entity.CartItemEntity;
import com.techora.domain.cart.service.CartService;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.idempotency.constant.IdempotencyOperation;
import com.techora.domain.idempotency.service.IdempotencyService;
import com.techora.domain.order.constant.OrderStatus;
import com.techora.domain.order.dto.dto.OrderWorkflowActor;
import com.techora.domain.order.dto.dto.OrderWorkflowTransition;
import com.techora.domain.order.dto.response.AdminOrderEventResponse;
import com.techora.domain.order.dto.response.OrderEventResponse;
import com.techora.domain.order.dto.response.OrderResponse;
import com.techora.domain.order.entity.OrderEntity;
import com.techora.domain.order.entity.OrderItemEntity;
import com.techora.domain.order.mapper.OrderMapper;
import com.techora.domain.order.repository.OrderRepository;
import com.techora.domain.outbox.service.OutboxEventService;
import com.techora.domain.product.entity.ProductEntity;
import com.techora.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final String USER_ID = "userId";

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final ProductService productService;
    private final OutboxEventService outboxEventService;
    private final OrderEventService orderEventService;
    private final IdempotencyService idempotencyService;
    private final OrderWorkflowService orderWorkflowService;

    @Transactional
    public OrderResponse checkout(UUID userId) {
        return checkoutWithoutIdempotency(userId);
    }

    @Transactional
    public OrderResponse checkout(UUID userId, String idempotencyKey) {
        return idempotencyService.execute(
                userId,
                idempotencyKey,
                IdempotencyOperation.CHECKOUT,
                Map.of(USER_ID, userId),
                OrderResponse.class,
                () -> checkoutWithoutIdempotency(userId)
        );
    }

    private OrderResponse checkoutWithoutIdempotency(UUID userId) {
        CartEntity cart = cartService.getRequiredCartWithItems(userId);
        validateCartHasItems(cart);
        validateCartItems(cart);

        OrderEntity order = buildOrder(cart);
        OrderEntity savedOrder = orderRepository.save(order);
        orderEventService.recordOrderPlaced(savedOrder);
        outboxEventService.recordOrderPlaced(savedOrder);
        OrderWorkflowTransition stockReserved = orderWorkflowService.reserveStock(savedOrder);
        cartService.clearCart(cart);
        return orderMapper.toResponse(stockReserved.order());
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(UUID userId, Integer page, Integer size) {
        Page<OrderEntity> orders = orderRepository.findByUserId(userId, pageRequest(page, size));
        return toPageResponse(orders);
    }

    @Transactional(readOnly = true)
    public OrderResponse getUserOrder(UUID userId, UUID orderId) {
        OrderEntity order = orderRepository.findWithItemsByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getAdminOrder(UUID orderId) {
        return orderMapper.toResponse(getRequiredOrder(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderEventResponse> getUserOrderEvents(UUID userId, UUID orderId) {
        getRequiredUserOrder(orderId, userId);
        return orderEventService.getUserEvents(orderId);
    }

    @Transactional(readOnly = true)
    public List<AdminOrderEventResponse> getAdminOrderEvents(UUID orderId) {
        getRequiredOrder(orderId);
        return orderEventService.getAdminEvents(orderId);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus nextStatus) {
        return updateStatus(orderId, nextStatus, null, null);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus nextStatus, UUID actorId, String actorName) {
        OrderEntity order = getRequiredOrder(orderId);
        OrderWorkflowTransition transition = transitionByActor(order, nextStatus, actorId, actorName);
        return orderMapper.toResponse(transition.order());
    }

    @Transactional
    public OrderResponse requestPayment(UUID orderId) {
        OrderEntity order = getRequiredOrder(orderId);
        return orderMapper.toResponse(orderWorkflowService.requestPayment(order).order());
    }

    @Transactional
    public OrderResponse markPaid(UUID orderId, UUID actorId, String actorName) {
        OrderEntity order = getRequiredOrder(orderId);
        OrderWorkflowActor actor = new OrderWorkflowActor(actorId, actorName);
        return orderMapper.toResponse(orderWorkflowService.confirmPayment(order, actor).order());
    }

    @Transactional
    public OrderResponse markPaymentFailed(UUID orderId, UUID actorId, String actorName) {
        OrderEntity order = getRequiredOrder(orderId);
        OrderWorkflowActor actor = new OrderWorkflowActor(actorId, actorName);
        return orderMapper.toResponse(orderWorkflowService.failPayment(order, actor).order());
    }

    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> countOrdersByStatus() {
        return orderRepository.countOrdersByStatus().stream()
                .collect(Collectors.toMap(
                        row -> (OrderStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    private void validateCartHasItems(CartEntity cart) {
        if (cart.getItems().isEmpty()) {
            throw new BusinessException(ResponseCode.CART_EMPTY);
        }
    }

    private void validateCartItems(CartEntity cart) {
        cart.getItems().forEach(this::validateCartItem);
    }

    private void validateCartItem(CartItemEntity item) {
        ProductEntity product = item.getProduct();
        if (!productService.isActive(product)) {
            throw new BusinessException(ResponseCode.PRODUCT_UNAVAILABLE);
        }
        if (product.getStockQuantity() < item.getQuantity()) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
    }

    private OrderEntity buildOrder(CartEntity cart) {
        Instant now = Instant.now();
        OrderEntity order = OrderEntity.builder()
                .user(cart.getUser())
                .status(OrderStatus.CREATED)
                .total(calculateTotal(cart))
                .createdAt(now)
                .updatedAt(now)
                .build();

        cart.getItems().stream()
                .map(item -> buildOrderItem(order, item))
                .forEach(order.getItems()::add);

        return order;
    }

    private OrderItemEntity buildOrderItem(OrderEntity order, CartItemEntity cartItem) {
        ProductEntity product = cartItem.getProduct();
        BigDecimal subtotal = calculateSubtotal(product.getPrice(), cartItem.getQuantity());

        return OrderItemEntity.builder()
                .order(order)
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .unitPrice(product.getPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(subtotal)
                .build();
    }

    private OrderEntity getRequiredOrder(UUID orderId) {
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }

    private OrderEntity getRequiredUserOrder(UUID orderId, UUID userId) {
        return orderRepository.findWithItemsByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }

    private OrderWorkflowTransition transitionByActor(
            OrderEntity order,
            OrderStatus nextStatus,
            UUID actorId,
            String actorName
    ) {
        if (actorId == null) {
            return orderWorkflowService.changeBySystem(order, nextStatus);
        }
        return orderWorkflowService.changeByAdmin(order, nextStatus, new OrderWorkflowActor(actorId, actorName));
    }

    private PageResponse<OrderResponse> toPageResponse(Page<OrderEntity> page) {
        return new PageResponse<>(
                page.getContent().stream().map(orderMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(resolvePage(page), resolveSize(size), DEFAULT_SORT);
    }

    private BigDecimal calculateTotal(CartEntity cart) {
        return cart.getItems().stream()
                .map(item -> calculateSubtotal(item.getProduct().getPrice(), item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateSubtotal(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private int resolvePage(Integer page) {
        return page == null || page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
    }

    private int resolveSize(Integer size) {
        if (size == null || size <= DEFAULT_PAGE) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
