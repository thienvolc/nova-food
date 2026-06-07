package com.nova.food.api.rest;

import com.nova.food.app.dto.response.ResponseDto;
import com.nova.food.app.service.ResponseFactory;
import com.nova.food.domain.restaurant.dto.request.CreateRestaurantRequest;
import com.nova.food.domain.restaurant.dto.request.UpdateRestaurantRequest;
import com.nova.food.domain.restaurant.dto.request.UpdateRestaurantStatusRequest;
import com.nova.food.domain.restaurant.constant.RestaurantStatus;
import com.nova.food.domain.restaurant.service.RestaurantService;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final ResponseFactory responseFactory;

    @PostMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto create(@AuthenticationPrincipal UserPrincipal principal,
                              @Valid @RequestBody CreateRestaurantRequest request) {
        return responseFactory.success(restaurantService.create(request, principal.getUserId()));
    }

    @GetMapping
    public ResponseDto list() {
        return responseFactory.success(restaurantService.listActive());
    }

    @GetMapping("/search")
    public ResponseDto search(@AuthenticationPrincipal UserPrincipal principal,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) RestaurantStatus status,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        return responseFactory.success(restaurantService.search(
                keyword,
                status,
                page,
                size,
                canFilterRestaurantStatus(principal)
        ));
    }

    @GetMapping("/{restaurantId}")
    public ResponseDto get(@PathVariable UUID restaurantId) {
        return responseFactory.success(restaurantService.get(restaurantId));
    }

    @PutMapping("/{restaurantId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto update(@AuthenticationPrincipal UserPrincipal principal,
                              @PathVariable UUID restaurantId,
                              @Valid @RequestBody UpdateRestaurantRequest request) {
        return responseFactory.success(restaurantService.update(
                restaurantId,
                request,
                principal.getUserId(),
                principal.hasRole("ADMIN")
        ));
    }

    @PatchMapping("/{restaurantId}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto updateStatus(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable UUID restaurantId,
                                    @Valid @RequestBody UpdateRestaurantStatusRequest request) {
        return responseFactory.success(restaurantService.updateStatus(
                restaurantId,
                request.status(),
                principal.getUserId(),
                principal.hasRole("ADMIN")
        ));
    }

    private boolean canFilterRestaurantStatus(UserPrincipal principal) {
        return principal != null && (principal.hasRole("ADMIN") || principal.hasRole("RESTAURANT_OWNER"));
    }
}
