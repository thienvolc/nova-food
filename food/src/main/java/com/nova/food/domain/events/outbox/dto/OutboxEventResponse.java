package com.nova.food.domain.events.outbox.dto;

import com.nova.food.domain.events.outbox.OutboxStatus;

import java.time.Instant;
import java.util.UUID;

public record OutboxEventResponse(
        UUID id,
        String eventType,
        String eventKey,
        String topic,
        UUID aggregateId,
        OutboxStatus status,
        int retryCount,
        Instant nextRetryAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}
