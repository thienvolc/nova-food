package com.nova.food.domain.delivery.dto.event;

import java.time.Instant;
import java.util.UUID;

public record DeliveryCompletedEvent(
        UUID deliveryId,
        UUID orderId,
        UUID driverId,
        Instant occurredAt
) {
}
