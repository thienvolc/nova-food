package com.techora.domain.payment.repository;

import com.techora.domain.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    boolean existsByOrderId(UUID orderId);

    @EntityGraph(attributePaths = {"order", "order.items", "user"})
    Optional<PaymentEntity> findWithOrderById(UUID paymentId);

    @EntityGraph(attributePaths = {"order", "order.items", "user"})
    Optional<PaymentEntity> findWithOrderByIdAndUserId(UUID paymentId, UUID userId);
}
