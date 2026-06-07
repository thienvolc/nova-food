package com.nova.food.api.rest;

import com.nova.food.app.dto.response.ResponseDto;
import com.nova.food.app.service.ResponseFactory;
import com.nova.food.domain.payment.dto.request.MockPaymentRequest;
import com.nova.food.domain.payment.service.PaymentService;
import com.nova.food.infrastructure.service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/orders/{orderId}/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final ResponseFactory responseFactory;

    @PostMapping("/mock")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseDto payMock(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable UUID orderId,
                               @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                               @Valid @RequestBody MockPaymentRequest request) {
        return responseFactory.success(paymentService.payMock(orderId, request, principal.getUserId(), idempotencyKey));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseDto listByOrder(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable UUID orderId,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return responseFactory.success(paymentService.listByOrder(
                orderId,
                principal.getUserId(),
                principal.hasRole("ADMIN"),
                page,
                size
        ));
    }
}
