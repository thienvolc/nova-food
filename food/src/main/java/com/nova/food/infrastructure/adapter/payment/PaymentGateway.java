package com.nova.food.infrastructure.adapter.payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {

    PaymentGatewayResult pay(UUID orderId, BigDecimal amount, Boolean approved);
}
