package com.nova.food.infrastructure.adapter.payment;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayResult pay(UUID orderId, BigDecimal amount, Boolean approved) {
        if (Boolean.FALSE.equals(approved)) {
            return new PaymentGatewayResult(false, null, "mock_payment_declined");
        }
        return new PaymentGatewayResult(true, "mock-" + UUID.randomUUID(), null);
    }
}
