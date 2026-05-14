package com.techora.domain.payment.mapper;

import com.techora.domain.payment.dto.response.PaymentResponse;
import com.techora.domain.payment.entity.PaymentEntity;
import org.springframework.stereotype.Service;

@Service
public class PaymentMapper {
    public PaymentResponse toResponse(PaymentEntity payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getUser().getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getProviderReference(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
