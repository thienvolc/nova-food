package com.techora.domain.inventory.repository;

import com.techora.domain.inventory.constant.InventoryReservationStatus;
import com.techora.domain.inventory.entity.InventoryReservationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, UUID> {
    @EntityGraph(attributePaths = {"product", "order"})
    List<InventoryReservationEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reservation from InventoryReservationEntity reservation
            join fetch reservation.product
            join fetch reservation.order
            where reservation.order.id = :orderId
            order by reservation.createdAt asc
            """)
    List<InventoryReservationEntity> findLockedByOrderId(@Param("orderId") UUID orderId);

    @Query("""
            select coalesce(sum(reservation.quantity), 0)
            from InventoryReservationEntity reservation
            where reservation.product.id = :productId
              and reservation.status = :status
            """)
    int sumQuantityByProductIdAndStatus(
            @Param("productId") UUID productId,
            @Param("status") InventoryReservationStatus status
    );

    @EntityGraph(attributePaths = {"product", "order"})
    List<InventoryReservationEntity> findByStatusAndExpiresAtBefore(
            InventoryReservationStatus status,
            Instant expiresAt
    );
}
