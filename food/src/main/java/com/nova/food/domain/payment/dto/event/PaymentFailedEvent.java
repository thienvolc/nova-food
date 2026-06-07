package com.nova.food.domain.payment.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        String reason,
        Instant occurredAt
) {
}
