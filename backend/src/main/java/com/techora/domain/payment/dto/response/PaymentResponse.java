package com.techora.domain.payment.dto.response;

import com.techora.domain.payment.constant.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        PaymentStatus status,
        String providerReference,
        Instant createdAt,
        Instant updatedAt
) {
}
