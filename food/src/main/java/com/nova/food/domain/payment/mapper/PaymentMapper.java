package com.nova.food.domain.payment.mapper;

import com.nova.food.domain.payment.dto.response.PaymentResponse;
import com.nova.food.domain.payment.entity.PaymentTransactionEntity;
import org.springframework.stereotype.Service;

@Service
public class PaymentMapper {

    public PaymentResponse toResponse(PaymentTransactionEntity payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getProvider(),
                payment.getProviderTransactionId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
