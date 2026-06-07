package com.nova.food.domain.menu.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateMenuItemStockRequest(
        boolean trackStock,
        @PositiveOrZero int stockQuantity,
        @PositiveOrZero int lowStockThreshold
) {
}
