package com.nova.food.domain.menu.service;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.common.dto.PageResponse;
import com.nova.food.domain.menu.dto.request.CreateStockAdjustmentRequest;
import com.nova.food.domain.menu.dto.request.CreateMenuItemRequest;
import com.nova.food.domain.menu.dto.request.UpdateMenuItemStockRequest;
import com.nova.food.domain.menu.dto.request.UpdateMenuItemRequest;
import com.nova.food.domain.menu.dto.response.MenuItemResponse;
import com.nova.food.domain.menu.entity.MenuItemEntity;
import com.nova.food.domain.menu.entity.StockAdjustmentEntity;
import com.nova.food.domain.menu.mapper.MenuItemMapper;
import com.nova.food.domain.menu.policy.InventoryPolicyService;
import com.nova.food.domain.menu.repository.MenuItemRepository;
import com.nova.food.domain.menu.repository.StockAdjustmentRepository;
import com.nova.food.domain.restaurant.entity.RestaurantEntity;
import com.nova.food.domain.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final MenuItemMapper menuItemMapper;
    private final InventoryPolicyService inventoryPolicyService;
    private final RestaurantService restaurantService;

    @Transactional
    public MenuItemResponse create(UUID restaurantId, CreateMenuItemRequest request, UUID userId, boolean admin) {
        inventoryPolicyService.validatePositivePrice(request.price());
        RestaurantEntity restaurant = restaurantService.getRequiredRestaurant(restaurantId);
        restaurantService.validateCanManage(restaurant, userId, admin);
        Instant now = Instant.now();
        MenuItemEntity menuItem = MenuItemEntity.builder()
                .id(UUID.randomUUID())
                .restaurantId(restaurantId)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .available(request.available())
                .trackStock(request.trackStock())
                .stockQuantity(request.stockQuantity())
                .lowStockThreshold(request.lowStockThreshold())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return menuItemMapper.toResponse(menuItemRepository.save(menuItem));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> listAvailableByRestaurant(UUID restaurantId) {
        restaurantService.getRequiredRestaurant(restaurantId);
        return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId).stream()
                .map(menuItemMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MenuItemResponse> search(UUID restaurantId,
                                                 Boolean available,
                                                 BigDecimal minPrice,
                                                 BigDecimal maxPrice,
                                                 String keyword,
                                                 int page,
                                                 int size,
                                                 boolean canFilterUnavailable) {
        validatePage(page, size);
        inventoryPolicyService.validatePriceRange(minPrice, maxPrice);
        validateAvailabilityFilter(available, canFilterUnavailable);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        var result = menuItemRepository.search(
                restaurantId,
                resolveAvailability(available),
                minPrice,
                maxPrice,
                clean(keyword),
                pageable
        ).map(menuItemMapper::toResponse);
        return PageResponse.from(result);
    }

    @Transactional
    public MenuItemResponse update(UUID menuItemId, UpdateMenuItemRequest request, UUID userId, boolean admin) {
        inventoryPolicyService.validatePositivePrice(request.price());
        MenuItemEntity menuItem = getRequiredMenuItem(menuItemId);
        RestaurantEntity restaurant = restaurantService.getRequiredRestaurant(menuItem.getRestaurantId());
        restaurantService.validateCanManage(restaurant, userId, admin);
        menuItem.update(request.name(), request.description(), request.price());
        return menuItemMapper.toResponse(menuItem);
    }

    @Transactional
    public MenuItemResponse updateAvailability(UUID menuItemId, boolean available, UUID userId, boolean admin) {
        MenuItemEntity menuItem = getRequiredMenuItem(menuItemId);
        RestaurantEntity restaurant = restaurantService.getRequiredRestaurant(menuItem.getRestaurantId());
        restaurantService.validateCanManage(restaurant, userId, admin);
        menuItem.updateAvailability(available);
        return menuItemMapper.toResponse(menuItem);
    }

    @Transactional
    public MenuItemResponse updateStock(UUID menuItemId, UpdateMenuItemStockRequest request, UUID userId, boolean admin) {
        MenuItemEntity menuItem = getMenuItemForMutation(menuItemId, userId, admin);
        menuItem.updateStock(request.trackStock(), request.stockQuantity(), request.lowStockThreshold());
        return menuItemMapper.toResponse(menuItem);
    }

    @Transactional
    public MenuItemResponse adjustStock(UUID menuItemId,
                                        CreateStockAdjustmentRequest request,
                                        UUID userId,
                                        boolean admin) {
        MenuItemEntity menuItem = getMenuItemForMutation(menuItemId, userId, admin);
        inventoryPolicyService.validateStockAfterAdjustment(menuItem, request.quantityDelta());
        menuItem.adjustStock(request.quantityDelta());
        stockAdjustmentRepository.save(StockAdjustmentEntity.builder()
                .id(UUID.randomUUID())
                .menuItemId(menuItem.getId())
                .quantityDelta(request.quantityDelta())
                .reason(clean(request.reason()))
                .changedByUserId(userId)
                .createdAt(Instant.now())
                .build());
        return menuItemMapper.toResponse(menuItem);
    }

    @Transactional(readOnly = true)
    public MenuItemEntity getRequiredMenuItem(UUID menuItemId) {
        return menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MENU_ITEM_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Map<UUID, MenuItemEntity> getRequiredMenuItems(Collection<UUID> menuItemIds) {
        Map<UUID, MenuItemEntity> menuItems = menuItemRepository.findByIdIn(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItemEntity::getId, Function.identity()));
        if (menuItems.size() != menuItemIds.size()) {
            throw new BusinessException(ResponseCode.MENU_ITEM_NOT_FOUND);
        }
        return menuItems;
    }

    public void decrementStock(MenuItemEntity menuItem, int quantity) {
        inventoryPolicyService.decrementStock(menuItem, quantity);
    }

    public void restoreStock(MenuItemEntity menuItem, int quantity) {
        inventoryPolicyService.restoreStock(menuItem, quantity);
    }

    private MenuItemEntity getMenuItemForMutation(UUID menuItemId, UUID userId, boolean admin) {
        MenuItemEntity menuItem = getRequiredMenuItem(menuItemId);
        RestaurantEntity restaurant = restaurantService.getRequiredRestaurant(menuItem.getRestaurantId());
        restaurantService.validateCanManage(restaurant, userId, admin);
        return menuItem;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessException(ResponseCode.INVALID_PAGE_REQUEST);
        }
    }

    private void validateAvailabilityFilter(Boolean available, boolean canFilterUnavailable) {
        if (Boolean.FALSE.equals(available) && !canFilterUnavailable) {
            throw new BusinessException(ResponseCode.ACCESS_DENIED);
        }
    }

    private Boolean resolveAvailability(Boolean available) {
        if (available == null) {
            return Boolean.TRUE;
        }
        return available;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
