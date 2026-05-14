package com.techora.infrastructure.config;

import com.techora.infrastructure.config.prop.MessagePolicyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MessagePolicyProperties.class)
public class MessagePolicyConfig {
}
