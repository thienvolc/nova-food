package com.nova.food.domain.order.dto.response;

import com.nova.food.domain.order.constant.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusHistoryResponse(
        UUID orderId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        UUID changedByUserId,
        String reason,
        Instant changedAt
) {
}
