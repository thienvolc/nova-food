package com.nova.food.domain.delivery.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignDeliveryRequest(
        @NotNull UUID driverId
) {
}
