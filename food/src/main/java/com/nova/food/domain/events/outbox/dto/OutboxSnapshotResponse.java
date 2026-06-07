package com.nova.food.domain.events.outbox.dto;

import java.time.Instant;

public record OutboxSnapshotResponse(
        long pendingOutboxCount,
        long failedOutboxCount,
        Instant latestPaidOrderTime,
        Instant latestCompletedDeliveryTime
) {
}
