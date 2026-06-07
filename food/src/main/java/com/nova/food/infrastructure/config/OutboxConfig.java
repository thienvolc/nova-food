package com.nova.food.infrastructure.config;

import com.nova.food.infrastructure.config.prop.OutboxRetryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OutboxRetryProperties.class)
public class OutboxConfig {
}
