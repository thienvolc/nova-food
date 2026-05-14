package com.techora.domain.order.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.inventory.service.InventoryReservationService;
import com.techora.domain.order.constant.OrderStatus;
import com.techora.domain.order.dto.dto.OrderWorkflowActor;
import com.techora.domain.order.dto.dto.OrderWorkflowTransition;
import com.techora.domain.order.entity.OrderEntity;
import com.techora.domain.order.repository.OrderRepository;
import com.techora.domain.outbox.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderWorkflowService {
    private final OrderRepository orderRepository;
    private final OrderStatusPolicy orderStatusPolicy;
    private final InventoryReservationService inventoryReservationService;
    private final OrderEventService orderEventService;
    private final OutboxEventService outboxEventService;

    public OrderWorkflowTransition reserveStock(OrderEntity order) {
        OrderWorkflowTransition transition = transition(order, OrderStatus.STOCK_RESERVED);
        inventoryReservationService.reserve(transition.order());
        recordSystemEvent(transition);
        recordOutboxEvent(transition);
        return transition;
    }

    public OrderWorkflowTransition requestPayment(OrderEntity order) {
        OrderWorkflowTransition transition = transition(order, OrderStatus.PAYMENT_PENDING);
        recordSystemEvent(transition);
        recordOutboxEvent(transition);
        return transition;
    }

    public OrderWorkflowTransition confirmPayment(OrderEntity order, OrderWorkflowActor actor) {
        OrderWorkflowTransition transition = transition(order, OrderStatus.PAID);
        inventoryReservationService.confirm(transition.order());
        orderEventService.recordUserStatusChanged(
                transition.order(),
                transition.oldStatus(),
                transition.newStatus(),
                actor.actorId(),
                actor.actorName()
        );
        recordOutboxEvent(transition);
        return transition;
    }

    public OrderWorkflowTransition failPayment(OrderEntity order, OrderWorkflowActor actor) {
        OrderWorkflowTransition paymentFailed = transition(order, OrderStatus.PAYMENT_FAILED);
        orderEventService.recordUserStatusChanged(
                paymentFailed.order(),
                paymentFailed.oldStatus(),
                paymentFailed.newStatus(),
                actor.actorId(),
                actor.actorName()
        );
        recordOutboxEvent(paymentFailed);
        return cancelAfterPaymentFailure(paymentFailed.order(), actor);
    }

    public OrderWorkflowTransition changeByAdmin(
            OrderEntity order,
            OrderStatus nextStatus,
            OrderWorkflowActor actor
    ) {
        OrderWorkflowTransition transition = transition(order, nextStatus);
        orderEventService.recordAdminStatusChanged(
                transition.order(),
                transition.oldStatus(),
                transition.newStatus(),
                actor.actorId(),
                actor.actorName()
        );
        releaseReservedStockOnCancellation(transition);
        recordOutboxEvent(transition);
        return transition;
    }

    public OrderWorkflowTransition cancelBySystem(OrderEntity order) {
        return changeBySystem(order, OrderStatus.CANCELLED);
    }

    public OrderWorkflowTransition changeBySystem(OrderEntity order, OrderStatus nextStatus) {
        OrderWorkflowTransition transition = transition(order, nextStatus);
        recordSystemEvent(transition);
        releaseReservedStockOnCancellation(transition);
        recordOutboxEvent(transition);
        return transition;
    }

    private OrderWorkflowTransition cancelAfterPaymentFailure(OrderEntity order, OrderWorkflowActor actor) {
        OrderWorkflowTransition transition = transition(order, OrderStatus.CANCELLED);
        inventoryReservationService.release(transition.order());
        orderEventService.recordUserStatusChanged(
                transition.order(),
                transition.oldStatus(),
                transition.newStatus(),
                actor.actorId(),
                actor.actorName()
        );
        recordOutboxEvent(transition);
        return transition;
    }

    private void recordOutboxEvent(OrderWorkflowTransition transition) {
        if (transition.newStatus() == OrderStatus.CANCELLED) {
            outboxEventService.recordOrderCancelled(transition.order());
            return;
        }
        outboxEventService.recordOrderStatusChanged(
                transition.order(),
                transition.oldStatus(),
                transition.newStatus()
        );
    }

    private void releaseReservedStockOnCancellation(OrderWorkflowTransition transition) {
        if (transition.newStatus() == OrderStatus.CANCELLED) {
            inventoryReservationService.release(transition.order());
        }
    }

    private void recordSystemEvent(OrderWorkflowTransition transition) {
        orderEventService.recordSystemStatusChanged(
                transition.order(),
                transition.oldStatus(),
                transition.newStatus()
        );
    }

    private OrderWorkflowTransition transition(OrderEntity order, OrderStatus nextStatus) {
        OrderStatus oldStatus = order.getStatus();
        validateTransition(oldStatus, nextStatus);
        order.setStatus(nextStatus);
        order.setUpdatedAt(Instant.now());
        return new OrderWorkflowTransition(orderRepository.save(order), oldStatus, nextStatus);
    }

    private void validateTransition(OrderStatus currentStatus, OrderStatus nextStatus) {
        if (!orderStatusPolicy.canTransition(currentStatus, nextStatus)) {
            throw new BusinessException(ResponseCode.INVALID_ORDER_STATUS_TRANSITION);
        }
    }
}
