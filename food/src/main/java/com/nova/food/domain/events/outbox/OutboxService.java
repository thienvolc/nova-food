package com.nova.food.domain.events.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.food.domain.delivery.dto.event.DeliveryCompletedEvent;
import com.nova.food.domain.order.dto.event.OrderReadyForDeliveryEvent;
import com.nova.food.domain.payment.dto.event.PaymentCompletedEvent;
import com.nova.food.domain.payment.dto.event.PaymentFailedEvent;
import com.nova.food.infrastructure.config.prop.EventPublisherProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final EventOutboxRepository eventOutboxRepository;
    private final ObjectMapper objectMapper;
    private final EventPublisherProperties eventPublisherProperties;

    @Transactional
    public void append(PaymentCompletedEvent event) {
        append(
                "payment.completed.v1",
                "payment-completed:" + event.paymentId(),
                eventPublisherProperties.topics().paymentCompleted(),
                event.orderId(),
                event
        );
    }

    @Transactional
    public void append(PaymentFailedEvent event) {
        append(
                "payment.failed.v1",
                "payment-failed:" + event.paymentId(),
                eventPublisherProperties.topics().paymentFailed(),
                event.orderId(),
                event
        );
    }

    @Transactional
    public void append(OrderReadyForDeliveryEvent event) {
        append(
                "order.ready-for-delivery.v1",
                "order-ready-for-delivery:" + event.orderId(),
                eventPublisherProperties.topics().orderReadyForDelivery(),
                event.orderId(),
                event
        );
    }

    @Transactional
    public void append(DeliveryCompletedEvent event) {
        append(
                "delivery.completed.v1",
                "delivery-completed:" + event.deliveryId(),
                eventPublisherProperties.topics().deliveryCompleted(),
                event.orderId(),
                event
        );
    }

    private void append(String eventType, String eventKey, String topic, UUID aggregateId, Object event) {
        if (eventOutboxRepository.existsByEventKey(eventKey)) {
            return;
        }
        Instant now = Instant.now();
        eventOutboxRepository.save(EventOutboxEntity.builder()
                .id(UUID.randomUUID())
                .eventType(eventType)
                .eventKey(eventKey)
                .topic(topic)
                .aggregateId(aggregateId)
                .payload(serialize(event))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox event", ex);
        }
    }
}
