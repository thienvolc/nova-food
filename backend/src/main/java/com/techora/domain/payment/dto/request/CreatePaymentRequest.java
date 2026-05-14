package com.techora.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID orderId
) {
}
