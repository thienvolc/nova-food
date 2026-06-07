package com.nova.food.domain.delivery.service;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.common.dto.PageResponse;
import com.nova.food.domain.common.service.IdempotencyKeyService;
import com.nova.food.domain.common.service.IdempotencyLockManager;
import com.nova.food.domain.common.service.PageValidator;
import com.nova.food.domain.delivery.constant.DeliveryStatus;
import com.nova.food.domain.delivery.dto.event.DeliveryCompletedEvent;
import com.nova.food.domain.delivery.dto.request.AssignDeliveryRequest;
import com.nova.food.domain.delivery.dto.response.DeliveryResponse;
import com.nova.food.domain.delivery.entity.DeliveryEntity;
import com.nova.food.domain.delivery.mapper.DeliveryMapper;
import com.nova.food.domain.delivery.policy.DeliveryPolicyService;
import com.nova.food.domain.delivery.repository.DeliveryRepository;
import com.nova.food.domain.events.outbox.OutboxService;
import com.nova.food.domain.order.constant.OrderStatus;
import com.nova.food.domain.order.entity.OrderEntity;
import com.nova.food.domain.order.service.OrderService;
import com.nova.food.domain.user.constant.UserRole;
import com.nova.food.domain.user.entity.UserEntity;
import com.nova.food.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryMapper deliveryMapper;
    private final DeliveryPolicyService deliveryPolicyService;
    private final OrderService orderService;
    private final IdempotencyLockManager idempotencyLockManager;
    private final IdempotencyKeyService idempotencyKeyService;
    private final DeliveryAssignmentIdempotencyService deliveryAssignmentIdempotencyService;
    private final OutboxService outboxService;
    private final UserService userService;
    private final PageValidator pageValidator;

    @Transactional
    public DeliveryResponse assign(UUID orderId, AssignDeliveryRequest request) {
        return assign(orderId, request, null);
    }

    @Transactional
    public DeliveryResponse assign(UUID orderId, AssignDeliveryRequest request, String idempotencyKey) {
        String normalizedKey = idempotencyKeyService.normalize(idempotencyKey);
        if (normalizedKey == null) {
            return assignInternal(orderId, request, null);
        }
        String lockKey = "delivery-assign:" + orderId + ":" + normalizedKey;
        return idempotencyLockManager.executeWithLock(
                lockKey,
                () -> assignInternal(orderId, request, normalizedKey)
        );
    }

    private DeliveryResponse assignInternal(UUID orderId, AssignDeliveryRequest request, String normalizedKey) {
        OrderEntity order = orderService.getRequiredOrder(orderId);
        var existing = deliveryAssignmentIdempotencyService.findExisting(orderId, normalizedKey);
        if (existing.isPresent()) {
            return deliveryMapper.toResponse(existing.get());
        }
        orderService.validateOrderStatus(order, OrderStatus.READY_FOR_DELIVERY);
        deliveryPolicyService.validateOrderNotAssigned(orderId);
        UserEntity driver = userService.getRequiredUser(request.driverId());
        userService.validateRole(driver, UserRole.DRIVER);
        deliveryPolicyService.validateDriverHasNoActiveDelivery(driver.getId());
        Instant now = Instant.now();
        try {
            DeliveryEntity delivery = DeliveryEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .driverId(driver.getId())
                    .idempotencyKey(normalizedKey)
                    .status(DeliveryStatus.ASSIGNED)
                    .assignedAt(now)
                    .updatedAt(now)
                    .build();
            return deliveryMapper.toResponse(deliveryRepository.save(delivery));
        } catch (DataIntegrityViolationException ex) {
            return deliveryAssignmentIdempotencyService.recoverFromUniqueViolation(orderId, normalizedKey, ex)
                    .map(deliveryMapper::toResponse)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> listAssignedToDriver(UUID driverId) {
        return deliveryRepository.findByDriverIdOrderByAssignedAtDesc(driverId).stream()
                .map(deliveryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<DeliveryResponse> listAssignedToDriver(UUID driverId, int page, int size) {
        pageValidator.validate(page, size);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));
        return PageResponse.from(deliveryRepository.findByDriverId(driverId, pageable).map(deliveryMapper::toResponse));
    }

    @Transactional
    public DeliveryResponse start(UUID deliveryId, UUID driverId) {
        DeliveryEntity delivery = getRequiredDelivery(deliveryId);
        deliveryPolicyService.validateAssignedDriver(delivery, driverId);
        deliveryPolicyService.requireStatus(delivery, DeliveryStatus.ASSIGNED);
        OrderEntity order = orderService.getRequiredOrder(delivery.getOrderId());
        orderService.validateOrderStatus(order, OrderStatus.READY_FOR_DELIVERY);
        delivery.markDelivering();
        orderService.markDelivering(order, driverId);
        return deliveryMapper.toResponse(delivery);
    }

    @Transactional
    public DeliveryResponse complete(UUID deliveryId, UUID driverId) {
        DeliveryEntity delivery = getRequiredDelivery(deliveryId);
        deliveryPolicyService.validateAssignedDriver(delivery, driverId);
        deliveryPolicyService.requireStatus(delivery, DeliveryStatus.DELIVERING);
        OrderEntity order = orderService.getRequiredOrder(delivery.getOrderId());
        orderService.validateOrderStatus(order, OrderStatus.DELIVERING);
        delivery.markCompleted();
        orderService.markCompleted(order, driverId);
        outboxService.append(new DeliveryCompletedEvent(
                delivery.getId(),
                delivery.getOrderId(),
                driverId,
                Instant.now()
        ));
        return deliveryMapper.toResponse(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getByOrder(UUID orderId, UUID userId, boolean admin, boolean driver) {
        DeliveryEntity delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ResponseCode.DELIVERY_NOT_FOUND));
        if (!admin) {
            if (driver) {
                deliveryPolicyService.validateAssignedDriver(delivery, userId);
            } else {
                OrderEntity order = orderService.getRequiredOrder(orderId);
                orderService.validateOwnsOrder(order, userId);
            }
        }
        return deliveryMapper.toResponse(delivery);
    }

    private DeliveryEntity getRequiredDelivery(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(ResponseCode.DELIVERY_NOT_FOUND));
    }
}
