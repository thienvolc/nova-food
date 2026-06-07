package com.nova.food.infrastructure.adapter.payment;

public record PaymentGatewayResult(
        boolean success,
        String providerTransactionId,
        String failureReason
) {
}
