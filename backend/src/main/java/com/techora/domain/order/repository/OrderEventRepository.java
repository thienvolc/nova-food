package com.techora.domain.order.repository;

import com.techora.domain.order.entity.OrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderEventRepository extends JpaRepository<OrderEventEntity, UUID> {
    List<OrderEventEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
