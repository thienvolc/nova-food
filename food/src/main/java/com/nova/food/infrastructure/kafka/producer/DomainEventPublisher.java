package com.nova.food.infrastructure.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.food.domain.order.dto.event.OrderCreatedEvent;
import com.nova.food.domain.payment.dto.event.PaymentCompletedEvent;
import com.nova.food.domain.payment.dto.event.PaymentFailedEvent;
import com.nova.food.infrastructure.config.prop.EventPublisherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final EventPublisherProperties eventPublisherProperties;

    public void publish(OrderCreatedEvent event) {
        publish(eventPublisherProperties.topics().orderCreated(), event.orderId().toString(), event);
    }

    public void publish(PaymentCompletedEvent event) {
        publish(eventPublisherProperties.topics().paymentCompleted(), event.paymentId().toString(), event);
    }

    public void publish(PaymentFailedEvent event) {
        publish(eventPublisherProperties.topics().paymentFailed(), event.paymentId().toString(), event);
    }

    public void publish(String topic, String key, String payload) {
        try {
            if ("kafka".equalsIgnoreCase(eventPublisherProperties.mode())) {
                kafkaTemplate.send(topic, key, payload);
                return;
            }
            if ("hybrid".equalsIgnoreCase(eventPublisherProperties.mode())) {
                log.info("Local event {} {} {}", topic, key, payload);
                kafkaTemplate.send(topic, key, payload);
                return;
            }
            log.info("Local event {} {} {}", topic, key, payload);
        } catch (RuntimeException ex) {
            if (!eventPublisherProperties.fallbackOnKafkaError()) {
                throw ex;
            }
            log.warn("Failed to publish event for topic {}", topic, ex);
        }
    }

    private void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            publish(topic, key, payload);
        } catch (JsonProcessingException ex) {
            if (!eventPublisherProperties.fallbackOnKafkaError()) {
                throw new IllegalStateException("Failed to serialize event", ex);
            }
            log.warn("Failed to serialize event for topic {}", topic, ex);
        }
    }
}
