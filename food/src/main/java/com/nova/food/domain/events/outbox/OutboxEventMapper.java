package com.nova.food.domain.events.outbox;

import com.nova.food.domain.events.outbox.dto.OutboxEventResponse;
import com.nova.food.domain.events.outbox.dto.OutboxReplayResponse;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventMapper {

    public OutboxEventResponse toResponse(EventOutboxEntity entity) {
        return new OutboxEventResponse(
                entity.getId(),
                entity.getEventType(),
                entity.getEventKey(),
                entity.getTopic(),
                entity.getAggregateId(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getNextRetryAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public OutboxReplayResponse toReplayResponse(EventOutboxEntity entity) {
        return new OutboxReplayResponse(
                entity.getId(),
                entity.getEventKey(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getLastError(),
                entity.getUpdatedAt()
        );
    }
}
