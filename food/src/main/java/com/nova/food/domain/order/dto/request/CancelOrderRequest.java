package com.nova.food.domain.order.dto.request;

import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @Size(max = 220) String reason
) {
}
