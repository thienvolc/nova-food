package com.nova.food.domain.menu.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateMenuItemRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        boolean available,
        boolean trackStock,
        @PositiveOrZero int stockQuantity,
        @PositiveOrZero int lowStockThreshold
) {

    public CreateMenuItemRequest(String name, String description, BigDecimal price, boolean available) {
        this(name, description, price, available, false, 0, 0);
    }
}
