package com.nova.food.domain.order.repository;

import com.nova.food.domain.order.entity.OrderStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistoryEntity, UUID> {

    List<OrderStatusHistoryEntity> findByOrderIdOrderByChangedAtAsc(UUID orderId);
}
