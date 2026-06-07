package com.nova.food.domain.order.constant;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    PREPARING,
    READY_FOR_DELIVERY,
    DELIVERING,
    COMPLETED,
    CANCELLED
}
