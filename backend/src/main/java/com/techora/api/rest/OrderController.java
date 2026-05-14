package com.techora.api.rest;

import com.techora.app.dto.response.ResponseDto;
import com.techora.app.service.ResponseFactory;
import com.techora.domain.order.service.OrderService;
import com.techora.infrastructure.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;
    private final ResponseFactory responseFactory;

    @PostMapping("/checkout")
    public ResponseDto checkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return responseFactory.success(orderService.checkout(principal.getUserId(), idempotencyKey));
    }

    @GetMapping
    public ResponseDto getOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return responseFactory.success(orderService.getUserOrders(principal.getUserId(), page, size));
    }

    @GetMapping("/{orderId}")
    public ResponseDto getOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId
    ) {
        return responseFactory.success(orderService.getUserOrder(principal.getUserId(), orderId));
    }

    @GetMapping("/{orderId}/events")
    public ResponseDto getOrderEvents(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId
    ) {
        return responseFactory.success(orderService.getUserOrderEvents(principal.getUserId(), orderId));
    }
}
