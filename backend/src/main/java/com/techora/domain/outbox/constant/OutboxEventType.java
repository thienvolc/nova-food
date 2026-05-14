package com.techora.domain.outbox.constant;

public enum OutboxEventType {
    ORDER_PLACED,
    ORDER_STATUS_CHANGED,
    STOCK_REDUCED,
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,
    ORDER_CANCELLED
}
