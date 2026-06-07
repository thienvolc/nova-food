package com.nova.food.domain.events.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventOutboxRepository extends JpaRepository<EventOutboxEntity, UUID> {

    boolean existsByEventKey(String eventKey);

    List<EventOutboxEntity> findTop50ByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            List<OutboxStatus> statuses,
            Instant nextRetryAt
    );

    List<EventOutboxEntity> findTop20ByStatusOrderByCreatedAtDesc(OutboxStatus status);

    Optional<EventOutboxEntity> findFirstByEventTypeOrderByCreatedAtDesc(String eventType);

    long countByStatus(OutboxStatus status);
}
