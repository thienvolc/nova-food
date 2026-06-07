package com.nova.food.domain.menu.mapper;

import com.nova.food.domain.menu.dto.response.MenuItemResponse;
import com.nova.food.domain.menu.entity.MenuItemEntity;
import org.springframework.stereotype.Service;

@Service
public class MenuItemMapper {

    public MenuItemResponse toResponse(MenuItemEntity menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getRestaurantId(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.isAvailable(),
                menuItem.isTrackStock(),
                menuItem.getStockQuantity(),
                menuItem.getLowStockThreshold(),
                menuItem.getCreatedAt(),
                menuItem.getUpdatedAt()
        );
    }
}
