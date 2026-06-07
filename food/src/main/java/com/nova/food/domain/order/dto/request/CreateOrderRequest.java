package com.nova.food.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotEmpty List<@Valid Item> items
) {

    public record Item(
            @NotNull UUID menuItemId,
            @Positive int quantity
    ) {
    }
}
