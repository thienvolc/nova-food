package com.techora.domain.outbox.service;

import com.techora.domain.outbox.constant.OutboxEventStatus;
import com.techora.domain.outbox.entity.OutboxEventEntity;
import com.techora.domain.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class OutboxPublisherService {
    private static final int DEFAULT_BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public int publishPendingEvents() {
        return publishPendingEvents(DEFAULT_BATCH_SIZE, event -> {});
    }

    @Transactional
    public int publishPendingEvents(int batchSize, Consumer<OutboxEventEntity> publisher) {
        List<OutboxEventEntity> events = findPendingEvents(batchSize);
        events.forEach(event -> publishEvent(event, publisher));
        return events.size();
    }

    private List<OutboxEventEntity> findPendingEvents(int batchSize) {
        return outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, Math.max(1, batchSize))
        );
    }

    private void publishEvent(OutboxEventEntity event, Consumer<OutboxEventEntity> publisher) {
        try {
            publisher.accept(event);
            markPublished(event);
        } catch (RuntimeException ex) {
            markFailed(event, ex);
        }
    }

    private void markPublished(OutboxEventEntity event) {
        Instant now = Instant.now();
        event.setStatus(OutboxEventStatus.PUBLISHED);
        event.setProcessedAt(now);
        event.setUpdatedAt(now);
        event.setLastError(null);
    }

    private void markFailed(OutboxEventEntity event, RuntimeException ex) {
        event.setStatus(OutboxEventStatus.FAILED);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setUpdatedAt(Instant.now());
        event.setLastError(ex.getMessage());
    }
}
