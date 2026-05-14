package com.techora.domain.outbox.service;

import com.techora.domain.outbox.constant.OutboxAggregateType;
import com.techora.domain.outbox.constant.OutboxEventStatus;
import com.techora.domain.outbox.constant.OutboxEventType;
import com.techora.domain.outbox.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventFactory {
    private final OutboxPayloadFactory payloadFactory;

    public OutboxEventEntity create(
            OutboxAggregateType aggregateType,
            UUID aggregateId,
            OutboxEventType eventType,
            Map<String, Object> attributes
    ) {
        Instant now = Instant.now();
        return OutboxEventEntity.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payloadFactory.createPayload(attributes))
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
