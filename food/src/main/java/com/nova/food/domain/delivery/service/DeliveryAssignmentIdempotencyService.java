package com.nova.food.domain.delivery.service;

import com.nova.food.domain.delivery.entity.DeliveryEntity;
import com.nova.food.domain.delivery.repository.DeliveryRepository;
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
public class DeliveryAssignmentIdempotencyService {
    private final DeliveryRepository deliveryRepository;
    private final EntityManager entityManager;

    public Optional<DeliveryEntity> findExisting(UUID orderId, String normalizedKey) {
        if (normalizedKey == null) {
            return Optional.empty();
        }
        return deliveryRepository.findByOrderIdAndIdempotencyKey(orderId, normalizedKey);
    }

    public Optional<DeliveryEntity> recoverFromUniqueViolation(
            UUID orderId,
            String normalizedKey,
            DataIntegrityViolationException exception
    ) {
        if (normalizedKey == null) {
            return Optional.empty();
        }
        log.info("Delivery assignment idempotency race recovered, orderId={}, key={}", orderId, normalizedKey);
        entityManager.clear();
        return deliveryRepository.findByOrderIdAndIdempotencyKey(orderId, normalizedKey)
                .or(() -> {
                    throw exception;
                });
    }
}
