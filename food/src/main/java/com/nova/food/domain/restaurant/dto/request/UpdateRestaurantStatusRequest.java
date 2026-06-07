package com.nova.food.domain.restaurant.dto.request;

import com.nova.food.domain.restaurant.constant.RestaurantStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRestaurantStatusRequest(@NotNull RestaurantStatus status) {
}
