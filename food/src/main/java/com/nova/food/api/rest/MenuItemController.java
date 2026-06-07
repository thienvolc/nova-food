package com.nova.food.api.rest;

import com.nova.food.app.dto.response.ResponseDto;
import com.nova.food.app.service.ResponseFactory;
import com.nova.food.domain.menu.dto.request.CreateStockAdjustmentRequest;
import com.nova.food.domain.menu.dto.request.CreateMenuItemRequest;
import com.nova.food.domain.menu.dto.request.UpdateMenuItemAvailabilityRequest;
import com.nova.food.domain.menu.dto.request.UpdateMenuItemStockRequest;
import com.nova.food.domain.menu.dto.request.UpdateMenuItemRequest;
import com.nova.food.domain.menu.service.MenuItemService;
import com.nova.food.infrastructure.service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MenuItemController {

    private final MenuItemService menuItemService;
    private final ResponseFactory responseFactory;

    @PostMapping("/restaurants/{restaurantId}/menu-items")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto create(@AuthenticationPrincipal UserPrincipal principal,
                              @PathVariable UUID restaurantId,
                              @Valid @RequestBody CreateMenuItemRequest request) {
        return responseFactory.success(menuItemService.create(
                restaurantId,
                request,
                principal.getUserId(),
                principal.hasRole("ADMIN")
        ));
    }

    @GetMapping("/restaurants/{restaurantId}/menu-items")
    public ResponseDto list(@PathVariable UUID restaurantId) {
        return responseFactory.success(menuItemService.listAvailableByRestaurant(restaurantId));
    }

    @GetMapping("/menu-items/search")
    public ResponseDto search(@AuthenticationPrincipal UserPrincipal principal,
                              @RequestParam(required = false) UUID restaurantId,
                              @RequestParam(required = false) Boolean available,
                              @RequestParam(required = false) BigDecimal minPrice,
                              @RequestParam(required = false) BigDecimal maxPrice,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        return responseFactory.success(menuItemService.search(
                restaurantId,
                available,
                minPrice,
                maxPrice,
                keyword,
                page,
                size,
                canFilterUnavailable(principal)
        ));
    }

    @PutMapping("/menu-items/{menuItemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto update(@AuthenticationPrincipal UserPrincipal principal,
                              @PathVariable UUID menuItemId,
                              @Valid @RequestBody UpdateMenuItemRequest request) {
        return responseFactory.success(menuItemService.update(
                menuItemId,
                request,
                principal.getUserId(),
                principal.hasRole("ADMIN")
        ));
    }

    private boolean canFilterUnavailable(UserPrincipal principal) {
        return principal != null && (principal.hasRole("ADMIN") || principal.hasRole("RESTAURANT_OWNER"));
    }

    @PatchMapping("/menu-items/{menuItemId}/availability")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto updateAvailability(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable UUID menuItemId,
                                          @Valid @RequestBody UpdateMenuItemAvailabilityRequest request) {
        return responseFactory.success(menuItemService.updateAvailability(
                menuItemId,
                request.available(),
                principal.getUserId(),
                principal.hasRole("ADMIN")
        ));
    }

    @PatchMapping("/menu-items/{menuItemId}/stock")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto updateStock(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable UUID menuItemId,
                                   @Valid @RequestBody UpdateMenuItemStockRequest request) {
        return responseFactory.success(menuItemService.updateStock(
                menuItemId,
                request,
                principal.getUserId(),
                principal.hasRole("ADMIN")
        ));
    }

    @PostMapping("/menu-items/{menuItemId}/stock-adjustments")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto adjustStock(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable UUID menuItemId,
                                   @Valid @RequestBody CreateStockAdjustmentRequest request) {
        return responseFactory.success(menuItemService.adjustStock(
                menuItemId,
                request,
                principal.getUserId(),
                principal.hasRole("ADMIN")
        ));
    }
}
