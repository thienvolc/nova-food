package com.nova.food.domain.restaurant.mapper;

import com.nova.food.domain.restaurant.dto.response.RestaurantResponse;
import com.nova.food.domain.restaurant.entity.RestaurantEntity;
import org.springframework.stereotype.Service;

@Service
public class RestaurantMapper {

    public RestaurantResponse toResponse(RestaurantEntity restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getOwnerId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.getStatus(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt()
        );
    }
}
