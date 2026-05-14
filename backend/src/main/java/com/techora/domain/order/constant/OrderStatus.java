package com.techora.domain.order.constant;

public enum OrderStatus {
    CREATED,
    STOCK_RESERVED,
    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,
    CANCELLED,
    FULFILLING,
    SHIPPED,
    DELIVERED
}
