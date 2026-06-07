package com.nova.food.domain.events.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
@RequiredArgsConstructor
public class OutboxPublisherWorker {

    private final OutboxPublishProcessor outboxPublishProcessor;

    @Scheduled(fixedDelayString = "${app.outbox.worker-delay-ms:2000}")
    @Transactional
    public void publishPending() {
        outboxPublishProcessor.publishReadyBatch();
    }
}
