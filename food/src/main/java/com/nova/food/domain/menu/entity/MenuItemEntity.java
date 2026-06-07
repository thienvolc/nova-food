package com.nova.food.domain.menu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "menu_items",
        indexes = {
                @Index(name = "idx_menu_items_restaurant_id", columnList = "restaurant_id"),
                @Index(name = "idx_menu_items_available", columnList = "available"),
                @Index(name = "idx_menu_items_price", columnList = "price")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MenuItemEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "restaurant_id", nullable = false, updatable = false)
    private UUID restaurantId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "available", nullable = false)
    private boolean available;

    @Column(name = "track_stock", nullable = false)
    private boolean trackStock;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void update(String name, String description, BigDecimal price) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.updatedAt = Instant.now();
    }

    public void updateAvailability(boolean available) {
        this.available = available;
        this.updatedAt = Instant.now();
    }

    public void updateStock(boolean trackStock, int stockQuantity, int lowStockThreshold) {
        this.trackStock = trackStock;
        this.stockQuantity = stockQuantity;
        this.lowStockThreshold = lowStockThreshold;
        this.updatedAt = Instant.now();
    }

    public void adjustStock(int quantityDelta) {
        this.stockQuantity += quantityDelta;
        this.updatedAt = Instant.now();
    }
}
