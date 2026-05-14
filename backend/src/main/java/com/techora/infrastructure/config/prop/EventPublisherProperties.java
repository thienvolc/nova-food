package com.techora.infrastructure.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.events")
public record EventPublisherProperties(String mode, String topic, boolean fallbackOnKafkaError) {
    public EventPublisherProperties {
        mode = (mode == null || mode.isBlank()) ? "local" : mode;
        topic = (topic == null || topic.isBlank()) ? "techora.events" : topic;
    }
}
