package com.nova.food.api.rest;

import com.nova.food.app.dto.response.ResponseDto;
import com.nova.food.app.service.ResponseFactory;
import com.nova.food.domain.delivery.dto.request.AssignDeliveryRequest;
import com.nova.food.domain.delivery.service.DeliveryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final ResponseFactory responseFactory;

    @PostMapping("/orders/{orderId}/deliveries/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseDto assign(@PathVariable UUID orderId,
                              @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                              @Valid @RequestBody AssignDeliveryRequest request) {
        return responseFactory.success(deliveryService.assign(orderId, request, idempotencyKey));
    }

    @GetMapping("/deliveries/my")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseDto listMyDeliveries(@AuthenticationPrincipal UserPrincipal principal,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return responseFactory.success(deliveryService.listAssignedToDriver(principal.getUserId(), page, size));
    }

    @PatchMapping("/deliveries/{deliveryId}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseDto start(@AuthenticationPrincipal UserPrincipal principal,
                             @PathVariable UUID deliveryId) {
        return responseFactory.success(deliveryService.start(deliveryId, principal.getUserId()));
    }

    @PatchMapping("/deliveries/{deliveryId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseDto complete(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable UUID deliveryId) {
        return responseFactory.success(deliveryService.complete(deliveryId, principal.getUserId()));
    }

    @GetMapping("/orders/{orderId}/deliveries")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER', 'ADMIN')")
    public ResponseDto getByOrder(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable UUID orderId) {
        return responseFactory.success(deliveryService.getByOrder(
                orderId,
                principal.getUserId(),
                principal.hasRole("ADMIN"),
                principal.hasRole("DRIVER")
        ));
    }
}
