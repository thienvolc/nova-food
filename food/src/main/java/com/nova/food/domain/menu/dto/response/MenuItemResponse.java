package com.nova.food.domain.menu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        UUID restaurantId,
        String name,
        String description,
        BigDecimal price,
        boolean available,
        boolean trackStock,
        int stockQuantity,
        int lowStockThreshold,
        Instant createdAt,
        Instant updatedAt
) {
}
