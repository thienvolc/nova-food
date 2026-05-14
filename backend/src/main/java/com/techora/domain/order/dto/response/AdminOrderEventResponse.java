package com.techora.domain.order.dto.response;

import com.techora.domain.order.constant.OrderEventActorType;
import com.techora.domain.order.constant.OrderEventType;
import com.techora.domain.order.constant.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminOrderEventResponse(
        UUID id,
        UUID orderId,
        OrderEventType eventType,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        String reason,
        String metadata,
        OrderEventActorType actorType,
        UUID actorId,
        String actorName,
        Instant createdAt
) {
}
