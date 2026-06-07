package com.nova.food.infrastructure.config;

import com.nova.food.infrastructure.config.prop.EventPublisherProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EventPublisherProperties.class)
public class EventsConfig {
}
