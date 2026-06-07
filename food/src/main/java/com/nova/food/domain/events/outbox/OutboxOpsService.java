package com.nova.food.domain.events.outbox;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.events.outbox.dto.OutboxEventResponse;
import com.nova.food.domain.events.outbox.dto.OutboxReplayResponse;
import com.nova.food.domain.events.outbox.dto.OutboxSnapshotResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxOpsService {

    private static final String PAYMENT_COMPLETED_EVENT_TYPE = "payment.completed.v1";
    private static final String DELIVERY_COMPLETED_EVENT_TYPE = "delivery.completed.v1";

    private final EventOutboxRepository eventOutboxRepository;
    private final OutboxEventMapper outboxEventMapper;
    private final OutboxPublishProcessor outboxPublishProcessor;

    @Transactional(readOnly = true)
    public OutboxSnapshotResponse snapshot() {
        long pendingOutboxCount = eventOutboxRepository.countByStatus(OutboxStatus.PENDING);
        long failedOutboxCount = eventOutboxRepository.countByStatus(OutboxStatus.FAILED);
        Instant latestPaidOrderTime = latestEventTime(PAYMENT_COMPLETED_EVENT_TYPE);
        Instant latestCompletedDeliveryTime = latestEventTime(DELIVERY_COMPLETED_EVENT_TYPE);
        return new OutboxSnapshotResponse(
                pendingOutboxCount,
                failedOutboxCount,
                latestPaidOrderTime,
                latestCompletedDeliveryTime
        );
    }

    @Transactional(readOnly = true)
    public List<OutboxEventResponse> listFailed() {
        return eventOutboxRepository.findTop20ByStatusOrderByCreatedAtDesc(OutboxStatus.FAILED).stream()
                .map(outboxEventMapper::toResponse)
                .toList();
    }

    @Transactional
    public OutboxReplayResponse replayFailed(UUID outboxId) {
        EventOutboxEntity outbox = eventOutboxRepository.findById(outboxId)
                .orElseThrow(() -> new BusinessException(ResponseCode.OUTBOX_EVENT_NOT_FOUND));
        validateReplayable(outbox);
        outbox.markPending(Instant.now());
        outboxPublishProcessor.publishSingle(outbox);
        log.info(
                "Outbox replay attempted, eventKey={}, eventType={}, aggregateId={}, outboxId={}, status={}",
                outbox.getEventKey(),
                outbox.getEventType(),
                outbox.getAggregateId(),
                outbox.getId(),
                outbox.getStatus()
        );
        return outboxEventMapper.toReplayResponse(outbox);
    }

    private Instant latestEventTime(String eventType) {
        return eventOutboxRepository.findFirstByEventTypeOrderByCreatedAtDesc(eventType)
                .map(EventOutboxEntity::getCreatedAt)
                .orElse(null);
    }

    private void validateReplayable(EventOutboxEntity outbox) {
        if (outbox.getStatus() != OutboxStatus.FAILED) {
            throw new BusinessException(ResponseCode.OUTBOX_REPLAY_NOT_ALLOWED);
        }
    }
}
