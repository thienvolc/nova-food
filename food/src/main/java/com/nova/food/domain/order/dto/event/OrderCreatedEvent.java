package com.nova.food.domain.order.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID customerId,
        UUID restaurantId,
        BigDecimal total,
        Instant occurredAt
) {
}
