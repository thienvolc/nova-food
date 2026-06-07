package com.nova.food.domain.order.dto.response;

import com.nova.food.domain.order.constant.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String trackingId,
        UUID customerId,
        UUID restaurantId,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal total,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
