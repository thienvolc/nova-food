package com.techora.domain.outbox.dto;

import java.util.Map;
import java.util.UUID;

public record OutboxPayload(
        UUID eventId,
        Map<String, Object> attributes
) {}
