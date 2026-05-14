package com.techora.domain.order.repository;

import com.techora.domain.order.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findWithItemsById(UUID orderId);

    @EntityGraph(attributePaths = {"user", "items"})
    Optional<OrderEntity> findWithItemsByIdAndUserId(UUID orderId, UUID userId);

    @EntityGraph(attributePaths = {"user", "items"})
    Page<OrderEntity> findByUserId(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    @Query("select orderEntity.status, count(orderEntity) from OrderEntity orderEntity group by orderEntity.status")
    List<Object[]> countOrdersByStatus();
}
