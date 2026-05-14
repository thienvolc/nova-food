package com.techora.domain.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techora.domain.outbox.dto.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxPayloadFactory {
    private final ObjectMapper objectMapper;

    public String createPayload(Map<String, Object> attributes) {
        return toJson(new OutboxPayload(UUID.randomUUID(), Map.copyOf(attributes)));
    }

    private String toJson(OutboxPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize outbox payload", ex);
        }
    }
}
