package com.nova.food.domain.order.entity;

import com.nova.food.domain.order.constant.OrderStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@Entity
@Table(
        name = "food_orders",
        indexes = {
                @Index(name = "idx_food_orders_customer_id", columnList = "customer_id"),
                @Index(name = "idx_food_orders_restaurant_id", columnList = "restaurant_id"),
                @Index(name = "idx_food_orders_status", columnList = "status"),
                @Index(name = "idx_food_orders_created_at", columnList = "created_at"),
                @Index(name = "idx_food_orders_tracking_id", columnList = "tracking_id")
        },
        uniqueConstraints = {
                @jakarta.persistence.UniqueConstraint(name = "idx_food_orders_customer_key", columnNames = {"customer_id", "idempotency_key"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "restaurant_id", nullable = false, updatable = false)
    private UUID restaurantId;

    @Column(name = "tracking_id", unique = true, length = 32)
    private String trackingId;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "stock_decremented", nullable = false)
    private boolean stockDecremented;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void assignTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public void markPaid() {
        this.status = OrderStatus.PAID;
        this.updatedAt = Instant.now();
    }

    public void markConfirmed() {
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void markPreparing() {
        this.status = OrderStatus.PREPARING;
        this.updatedAt = Instant.now();
    }

    public void markReadyForDelivery() {
        this.status = OrderStatus.READY_FOR_DELIVERY;
        this.updatedAt = Instant.now();
    }

    public void markDelivering() {
        this.status = OrderStatus.DELIVERING;
        this.updatedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void markCancelled() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void markStockDecremented() {
        this.stockDecremented = true;
        this.updatedAt = Instant.now();
    }

    public void markStockRestored() {
        this.stockDecremented = false;
        this.updatedAt = Instant.now();
    }
}
