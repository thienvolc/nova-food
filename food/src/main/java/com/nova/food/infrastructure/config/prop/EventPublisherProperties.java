package com.nova.food.infrastructure.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.events")
public record EventPublisherProperties(String mode, Topics topics, boolean fallbackOnKafkaError) {
    public EventPublisherProperties {
        mode = (mode == null || mode.isBlank()) ? "local" : mode;
        topics = topics == null ? new Topics(null, null, null, null, null) : topics;
    }

    public record Topics(
            String orderCreated,
            String paymentCompleted,
            String paymentFailed,
            String orderReadyForDelivery,
            String deliveryCompleted
    ) {
        public Topics {
            orderCreated = (orderCreated == null || orderCreated.isBlank()) ? "nova.order.created" : orderCreated;
            paymentCompleted = (paymentCompleted == null || paymentCompleted.isBlank()) ? "nova.payment.completed" : paymentCompleted;
            paymentFailed = (paymentFailed == null || paymentFailed.isBlank()) ? "nova.payment.failed" : paymentFailed;
            orderReadyForDelivery = (orderReadyForDelivery == null || orderReadyForDelivery.isBlank())
                    ? "nova.order.ready-for-delivery" : orderReadyForDelivery;
            deliveryCompleted = (deliveryCompleted == null || deliveryCompleted.isBlank())
                    ? "nova.delivery.completed" : deliveryCompleted;
        }
    }
}
