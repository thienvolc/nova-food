package com.nova.food.domain.events.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@Entity
@Table(
        name = "event_outbox",
        indexes = {
                @Index(name = "idx_event_outbox_status_next_retry", columnList = "status,next_retry_at"),
                @Index(name = "idx_event_outbox_aggregate_id", columnList = "aggregate_id"),
                @Index(name = "idx_event_outbox_event_key", columnList = "event_key", unique = true)
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EventOutboxEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "event_key", nullable = false, unique = true, length = 120)
    private String eventKey;

    @Column(name = "topic", nullable = false, length = 120)
    private String topic;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void markProcessing(Instant now) {
        this.status = OutboxStatus.PROCESSING;
        this.updatedAt = now;
    }

    public void markSent(Instant now) {
        this.status = OutboxStatus.SENT;
        this.lastError = null;
        this.updatedAt = now;
    }

    public void markPending(Instant now) {
        this.status = OutboxStatus.PENDING;
        this.lastError = null;
        this.nextRetryAt = now;
        this.updatedAt = now;
    }

    public void markFailed(String error, Instant nextRetryAt, Instant now) {
        this.status = OutboxStatus.FAILED;
        this.retryCount += 1;
        this.lastError = error;
        this.nextRetryAt = nextRetryAt;
        this.updatedAt = now;
    }
}
