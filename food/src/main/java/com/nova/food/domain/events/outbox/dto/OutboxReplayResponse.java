package com.nova.food.domain.events.outbox.dto;

import com.nova.food.domain.events.outbox.OutboxStatus;

import java.time.Instant;
import java.util.UUID;

public record OutboxReplayResponse(
        UUID id,
        String eventKey,
        OutboxStatus status,
        int retryCount,
        String lastError,
        Instant updatedAt
) {
}
