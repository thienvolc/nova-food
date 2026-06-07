package com.nova.food.domain.payment.repository;

import com.nova.food.domain.payment.entity.PaymentTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, UUID> {

    List<PaymentTransactionEntity> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Page<PaymentTransactionEntity> findByOrderId(UUID orderId, Pageable pageable);

    Optional<PaymentTransactionEntity> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Optional<PaymentTransactionEntity> findByOrderIdAndIdempotencyKey(UUID orderId, String idempotencyKey);
}
