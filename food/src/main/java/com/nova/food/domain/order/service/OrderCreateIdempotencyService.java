package com.nova.food.domain.order.service;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.order.entity.OrderEntity;
import com.nova.food.domain.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCreateIdempotencyService {

    private final OrderRepository orderRepository;
    private final EntityManager entityManager;

    public Optional<OrderEntity> findExisting(UUID customerId, String normalizedKey) {
        if (normalizedKey == null) {
            return Optional.empty();
        }
        return orderRepository.findByCustomerIdAndIdempotencyKey(customerId, normalizedKey);
    }

    public OrderEntity requireSameRequest(OrderEntity order, String requestFingerprint) {
        if (!Objects.equals(order.getRequestFingerprint(), requestFingerprint)) {
            throw new BusinessException(ResponseCode.ORDER_IDEMPOTENCY_KEY_REUSED);
        }
        return order;
    }

    public Optional<OrderEntity> recoverFromUniqueViolation(
            UUID customerId,
            String normalizedKey,
            DataIntegrityViolationException exception
    ) {
        if (normalizedKey == null) {
            return Optional.empty();
        }
        log.info("Order creation idempotency race recovered, customerId={}, key={}", customerId, normalizedKey);
        entityManager.clear();
        return orderRepository.findByCustomerIdAndIdempotencyKey(customerId, normalizedKey)
                .or(() -> {
                    throw exception;
                });
    }
}
