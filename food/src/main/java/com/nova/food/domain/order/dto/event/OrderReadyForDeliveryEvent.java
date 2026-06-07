package com.nova.food.domain.order.dto.event;

import java.time.Instant;
import java.util.UUID;

public record OrderReadyForDeliveryEvent(
        UUID orderId,
        UUID restaurantId,
        UUID changedByUserId,
        Instant occurredAt
) {
}
