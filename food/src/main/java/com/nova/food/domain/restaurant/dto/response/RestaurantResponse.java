package com.nova.food.domain.restaurant.dto.response;

import com.nova.food.domain.restaurant.constant.RestaurantStatus;

import java.time.Instant;
import java.util.UUID;

public record RestaurantResponse(
        UUID id,
        UUID ownerId,
        String name,
        String description,
        String address,
        String phone,
        RestaurantStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
