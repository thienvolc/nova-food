package com.techora.domain.idempotency.constant;

public enum IdempotencyOperation {
    CHECKOUT,
    PAYMENT_CONFIRM,
    PAYMENT_FAIL
}
