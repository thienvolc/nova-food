package com.nova.food.domain.menu.policy;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.menu.entity.MenuItemEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class InventoryPolicyService {

    public UUID validateMenuItemsForOrder(Map<UUID, MenuItemEntity> menuItems, Map<UUID, Integer> requestedQuantities) {
        UUID restaurantId = null;
        for (MenuItemEntity menuItem : menuItems.values()) {
            validateAvailableForOrder(menuItem, requestedQuantities.get(menuItem.getId()));
            if (restaurantId == null) {
                restaurantId = menuItem.getRestaurantId();
            } else if (!restaurantId.equals(menuItem.getRestaurantId())) {
                throw new BusinessException(ResponseCode.ORDER_ITEMS_MUST_BE_SAME_RESTAURANT);
            }
        }
        return restaurantId;
    }

    public void validateAvailableForOrder(MenuItemEntity menuItem, Integer requestedQuantity) {
        if (!menuItem.isAvailable()) {
            throw new BusinessException(ResponseCode.MENU_ITEM_UNAVAILABLE);
        }
        if (requestedQuantity != null && menuItem.isTrackStock() && menuItem.getStockQuantity() < requestedQuantity) {
            throw new BusinessException(ResponseCode.MENU_ITEM_INSUFFICIENT_STOCK);
        }
    }

    public void validatePositivePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCode.INVALID_MENU_ITEM_PRICE);
        }
    }

    public void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResponseCode.INVALID_MENU_ITEM_PRICE);
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResponseCode.INVALID_MENU_ITEM_PRICE);
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(ResponseCode.INVALID_MENU_ITEM_PRICE);
        }
    }

    public void validateStockAfterAdjustment(MenuItemEntity menuItem, int quantityDelta) {
        if (menuItem.getStockQuantity() + quantityDelta < 0) {
            throw new BusinessException(ResponseCode.INVALID_STOCK_QUANTITY);
        }
    }

    public void decrementStock(MenuItemEntity menuItem, int quantity) {
        if (!menuItem.isTrackStock()) {
            return;
        }
        if (menuItem.getStockQuantity() < quantity) {
            throw new BusinessException(ResponseCode.MENU_ITEM_INSUFFICIENT_STOCK);
        }
        menuItem.adjustStock(-quantity);
    }

    public void restoreStock(MenuItemEntity menuItem, int quantity) {
        if (menuItem.isTrackStock()) {
            menuItem.adjustStock(quantity);
        }
    }
}
