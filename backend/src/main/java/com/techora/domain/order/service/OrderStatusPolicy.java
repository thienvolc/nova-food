package com.techora.domain.order.service;

import com.techora.domain.order.constant.OrderStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class OrderStatusPolicy {
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.STOCK_RESERVED, OrderStatus.CANCELLED),
            OrderStatus.STOCK_RESERVED, Set.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED),
            OrderStatus.PAYMENT_PENDING, Set.of(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED),
            OrderStatus.PAID, Set.of(OrderStatus.FULFILLING, OrderStatus.CANCELLED),
            OrderStatus.PAYMENT_FAILED, Set.of(OrderStatus.CANCELLED),
            OrderStatus.CANCELLED, Set.of(),
            OrderStatus.FULFILLING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of()
    );

    public boolean canTransition(OrderStatus currentStatus, OrderStatus nextStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus);
    }
}
