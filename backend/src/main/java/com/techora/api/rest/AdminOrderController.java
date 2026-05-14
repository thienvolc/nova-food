package com.techora.api.rest;

import com.techora.app.dto.response.ResponseDto;
import com.techora.app.service.ResponseFactory;
import com.techora.domain.order.dto.request.UpdateOrderStatusRequest;
import com.techora.domain.order.service.OrderService;
import com.techora.infrastructure.service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
    private final OrderService orderService;
    private final ResponseFactory responseFactory;

    @GetMapping("/{orderId}")
    public ResponseDto getOrder(@PathVariable UUID orderId) {
        return responseFactory.success(orderService.getAdminOrder(orderId));
    }

    @GetMapping("/status-summary")
    public ResponseDto getStatusSummary() {
        return responseFactory.success(orderService.countOrdersByStatus());
    }

    @GetMapping("/{orderId}/events")
    public ResponseDto getOrderEvents(@PathVariable UUID orderId) {
        return responseFactory.success(orderService.getAdminOrderEvents(orderId));
    }

    @PutMapping("/{orderId}/status")
    public ResponseDto updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return responseFactory.success(orderService.updateStatus(
                orderId,
                request.status(),
                principal.getUserId(),
                principal.getUsername()
        ));
    }
}
