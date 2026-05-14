package com.techora.domain.payment.service;

import com.techora.domain.payment.constant.PaymentStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class PaymentStatusPolicy {
    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentStatus.PENDING, Set.of(PaymentStatus.PAID, PaymentStatus.FAILED),
            PaymentStatus.PAID, Set.of(PaymentStatus.REFUNDED),
            PaymentStatus.FAILED, Set.of(),
            PaymentStatus.REFUNDED, Set.of()
    );

    public boolean canTransition(PaymentStatus currentStatus, PaymentStatus nextStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus);
    }
}
