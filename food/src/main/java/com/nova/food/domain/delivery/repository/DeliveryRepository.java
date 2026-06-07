package com.nova.food.domain.delivery.repository;

import com.nova.food.domain.delivery.entity.DeliveryEntity;
import com.nova.food.domain.delivery.constant.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, UUID> {

    boolean existsByOrderId(UUID orderId);

    List<DeliveryEntity> findByDriverIdOrderByAssignedAtDesc(UUID driverId);

    Page<DeliveryEntity> findByDriverId(UUID driverId, Pageable pageable);

    Optional<DeliveryEntity> findByOrderId(UUID orderId);

    Optional<DeliveryEntity> findByOrderIdAndIdempotencyKey(UUID orderId, String idempotencyKey);

    boolean existsByDriverIdAndStatusIn(UUID driverId, Collection<DeliveryStatus> statuses);
}
