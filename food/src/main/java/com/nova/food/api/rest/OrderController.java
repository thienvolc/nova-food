package com.nova.food.api.rest.rest;

import com.nova.food.app.dto.response.ResponseDto;
import com.nova.food.app.service.ResponseFactory;
import com.nova.food.domain.order.dto.request.CancelOrderRequest;
import com.nova.food.domain.order.dto.request.CreateOrderRequest;
import com.nova.food.domain.order.service.OrderService;
import com.nova.food.infrastructure.service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final ResponseFactory responseFactory;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseDto create(@AuthenticationPrincipal UserPrincipal principal,
                              @Valid @RequestBody CreateOrderRequest request,
                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return responseFactory.success(orderService.create(request, principal.getUserId(), idempotencyKey));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseDto get(@AuthenticationPrincipal UserPrincipal principal,
                           @PathVariable UUID orderId) {
        return responseFactory.success(orderService.get(orderId, principal.getUserId(), principal.hasRole("ADMIN")));
    }

    @GetMapping("/tracking/{trackingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto track(@AuthenticationPrincipal UserPrincipal principal,
                             @PathVariable String trackingId) {
        return responseFactory.success(orderService.track(
                trackingId,
                principal.getUserId(),
                principal.hasRole("ADMIN"),
                principal.hasRole("RESTAURANT_OWNER")
        ));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseDto listMyOrders(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return responseFactory.success(orderService.listMyOrders(principal.getUserId(), page, size));
    }

    @GetMapping("/restaurants/{restaurantId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto listRestaurantOrders(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable UUID restaurantId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return responseFactory.success(orderService.listRestaurantOrders(
                restaurantId,
                principal.getUserId(),
                principal.hasRole("ADMIN"),
                page,
                size
        ));
    }

    @GetMapping("/{orderId}/status-history")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto listStatusHistory(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable UUID orderId) {
        return responseFactory.success(orderService.listStatusHistory(
                orderId,
                principal.getUserId(),
                principal.hasRole("ADMIN"),
                principal.hasRole("RESTAURANT_OWNER")
        ));
    }

    @PatchMapping("/{orderId}/confirm")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto confirm(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable UUID orderId) {
        return responseFactory.success(orderService.confirm(
                orderId,
                principal.getUserId(),
                principal.hasRole("ADMIN")
        ));
    }

    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseDto cancelByCustomer(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable UUID orderId,
                                        @Valid @RequestBody CancelOrderRequest request) {
        return responseFactory.success(orderService.cancelByCustomer(orderId, request, principal.getUserId()));
    }

    @PatchMapping("/{orderId}/restaurant-cancel")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto cancelByRestaurant(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable UUID orderId,
                                          @Valid @RequestBody CancelOrderRequest request) {
        return responseFactory.success(orderService.cancelByRestaurant(
                orderId,
                request,
                principal.getUserId(),
                principal.hasRole("ADMIN")
        ));
    }

    @PatchMapping("/{orderId}/preparing")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto markPreparing(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable UUID orderId) {
        return responseFactory.success(orderService.markPreparing(
                orderId,
                principal.getUserId(),
                principal.hasRole("ADMIN")
        ));
    }

    @PatchMapping("/{orderId}/ready-for-delivery")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto markReadyForDelivery(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable UUID orderId) {
        return responseFactory.success(orderService.markReadyForDelivery(
                orderId,
                principal.getUserId(),
                principal.hasRole("ADMIN")
        ));
    }
}
