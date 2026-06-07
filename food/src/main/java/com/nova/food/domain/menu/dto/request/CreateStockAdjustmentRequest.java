package com.nova.food.domain.menu.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStockAdjustmentRequest(
        @NotNull Integer quantityDelta,
        @Size(max = 220) String reason
) {
}
