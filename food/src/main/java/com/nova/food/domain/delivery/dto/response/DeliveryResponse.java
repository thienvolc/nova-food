package com.nova.food.domain.delivery.dto.response;

import com.nova.food.domain.delivery.constant.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID orderId,
        UUID driverId,
        DeliveryStatus status,
        Instant assignedAt,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt
) {
}
