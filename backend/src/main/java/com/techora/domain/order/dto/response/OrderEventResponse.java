package com.techora.domain.order.dto.response;

import com.techora.domain.order.constant.OrderEventType;
import com.techora.domain.order.constant.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderEventResponse(
        UUID id,
        UUID orderId,
        OrderEventType eventType,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        String reason,
        Instant createdAt
) {
}
