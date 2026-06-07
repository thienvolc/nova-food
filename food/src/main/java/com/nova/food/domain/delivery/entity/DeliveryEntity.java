package com.nova.food.domain.delivery.entity;

import com.nova.food.domain.delivery.constant.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@Entity
@Table(
        name = "deliveries",
        indexes = {
                @Index(name = "idx_deliveries_order_id", columnList = "order_id"),
                @Index(name = "idx_deliveries_driver_id", columnList = "driver_id"),
                @Index(name = "idx_deliveries_driver_status", columnList = "driver_id,status"),
                @Index(name = "idx_deliveries_order_key", columnList = "order_id,idempotency_key", unique = true)
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeliveryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true, updatable = false)
    private UUID orderId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void markDelivering() {
        Instant now = Instant.now();
        this.status = DeliveryStatus.DELIVERING;
        this.startedAt = now;
        this.updatedAt = now;
    }

    public void markCompleted() {
        Instant now = Instant.now();
        this.status = DeliveryStatus.COMPLETED;
        this.completedAt = now;
        this.updatedAt = now;
    }
}
