package com.nova.food.domain.order.dto.response;

import com.nova.food.domain.order.constant.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderTrackingResponse(
        String trackingId,
        UUID orderId,
        OrderStatus status,
        UUID restaurantId,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt
) {
}
