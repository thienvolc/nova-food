package com.techora.api.rest;

import com.techora.app.dto.response.ResponseDto;
import com.techora.app.service.ResponseFactory;
import com.techora.domain.payment.dto.request.CreatePaymentRequest;
import com.techora.domain.payment.service.PaymentService;
import com.techora.infrastructure.service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final ResponseFactory responseFactory;

    @PostMapping
    public ResponseDto createPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        return responseFactory.success(paymentService.createPayment(principal.getUserId(), request));
    }

    @GetMapping("/{paymentId}")
    public ResponseDto getPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentId
    ) {
        return responseFactory.success(paymentService.getPayment(principal.getUserId(), paymentId));
    }

    @PostMapping("/{paymentId}/confirm")
    public ResponseDto confirmPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return responseFactory.success(paymentService.confirmPayment(principal.getUserId(), paymentId, idempotencyKey));
    }

    @PostMapping("/{paymentId}/fail")
    public ResponseDto failPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return responseFactory.success(paymentService.failPayment(principal.getUserId(), paymentId, idempotencyKey));
    }
}
