package com.nova.food.domain.events.outbox;

import com.nova.food.infrastructure.config.prop.OutboxRetryProperties;
import com.nova.food.infrastructure.kafka.producer.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxPublishProcessor {

    private final EventOutboxRepository eventOutboxRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OutboxRetryProperties outboxRetryProperties;

    public void publishReadyBatch() {
        List<EventOutboxEntity> candidates = eventOutboxRepository
                .findTop50ByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                        Instant.now()
                );
        for (EventOutboxEntity outbox : candidates) {
            publishSingle(outbox);
        }
    }

    public void publishSingle(EventOutboxEntity outbox) {
        Instant now = Instant.now();
        outbox.markProcessing(now);
        try {
            domainEventPublisher.publish(outbox.getTopic(), outbox.getEventKey(), outbox.getPayload());
            outbox.markSent(Instant.now());
            log.info(
                    "Outbox publish succeeded, eventKey={}, eventType={}, topic={}, aggregateId={}, outboxId={}",
                    outbox.getEventKey(),
                    outbox.getEventType(),
                    outbox.getTopic(),
                    outbox.getAggregateId(),
                    outbox.getId()
            );
        } catch (RuntimeException ex) {
            Instant nextRetryAt = nextRetryAt(outbox.getRetryCount() + 1, now);
            outbox.markFailed(shortMessage(ex), nextRetryAt, now);
            if (outbox.getRetryCount() > outboxRetryProperties.maxRetries()) {
                log.error(
                        "Outbox publish exceeded retry budget, eventKey={}, eventType={}, aggregateId={}, outboxId={}, retryCount={}",
                        outbox.getEventKey(),
                        outbox.getEventType(),
                        outbox.getAggregateId(),
                        outbox.getId(),
                        outbox.getRetryCount(),
                        ex
                );
                return;
            }
            log.warn(
                    "Outbox publish failed, eventKey={}, eventType={}, aggregateId={}, outboxId={}, retryCount={}",
                    outbox.getEventKey(),
                    outbox.getEventType(),
                    outbox.getAggregateId(),
                    outbox.getId(),
                    outbox.getRetryCount(),
                    ex
            );
        }
    }

    private Instant nextRetryAt(int retryCount, Instant now) {
        long delaySeconds = Math.min(
                outboxRetryProperties.maxBackoffSeconds(),
                outboxRetryProperties.stepBackoffSeconds() * retryCount
        );
        return now.plusSeconds(Math.max(1L, delaySeconds));
    }

    private String shortMessage(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
