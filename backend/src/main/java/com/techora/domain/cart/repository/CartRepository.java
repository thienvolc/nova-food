package com.techora.domain.cart.repository;

import com.techora.domain.cart.entity.CartEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<CartEntity, UUID> {
    @EntityGraph(attributePaths = {"user", "items", "items.product", "items.product.category"})
    Optional<CartEntity> findWithItemsByUserId(UUID userId);
}
