package com.nova.food.domain.payment.dto.response;

import com.nova.food.domain.payment.constant.PaymentMethod;
import com.nova.food.domain.payment.constant.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        PaymentMethod method,
        String provider,
        String providerTransactionId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
