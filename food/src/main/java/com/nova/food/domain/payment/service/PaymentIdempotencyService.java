package com.nova.food.domain.payment.service;

import com.nova.food.domain.payment.entity.PaymentTransactionEntity;
import com.nova.food.domain.payment.repository.PaymentTransactionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentIdempotencyService {
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final EntityManager entityManager;

    public Optional<PaymentTransactionEntity> findExisting(UUID orderId, String normalizedKey) {
        if (normalizedKey == null) {
            return Optional.empty();
        }
        return paymentTransactionRepository.findByOrderIdAndIdempotencyKey(orderId, normalizedKey);
    }

    public Optional<PaymentTransactionEntity> recoverFromUniqueViolation(
            UUID orderId,
            String normalizedKey,
            DataIntegrityViolationException exception
    ) {
        if (normalizedKey == null) {
            return Optional.empty();
        }
        log.info("Payment idempotency race recovered, orderId={}, key={}", orderId, normalizedKey);
        entityManager.clear();
        return paymentTransactionRepository.findByOrderIdAndIdempotencyKey(orderId, normalizedKey)
                .or(() -> {
                    throw exception;
                });
    }
}
