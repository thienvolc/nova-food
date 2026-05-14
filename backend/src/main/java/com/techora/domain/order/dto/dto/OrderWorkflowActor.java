package com.techora.domain.order.dto.dto;

import java.util.UUID;

public record OrderWorkflowActor(
        UUID actorId,
        String actorName
) {
}
