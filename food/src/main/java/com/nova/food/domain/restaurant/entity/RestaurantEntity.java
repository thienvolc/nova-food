package com.nova.food.domain.restaurant.entity;

import com.nova.food.domain.restaurant.constant.RestaurantStatus;
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
        name = "restaurants",
        indexes = {
                @Index(name = "idx_restaurants_name", columnList = "name"),
                @Index(name = "idx_restaurants_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RestaurantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RestaurantStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void update(String name, String description, String address, String phone) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.phone = phone;
        this.updatedAt = Instant.now();
    }

    public void updateStatus(RestaurantStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
}
