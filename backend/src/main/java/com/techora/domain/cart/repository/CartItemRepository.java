package com.techora.domain.cart.repository;

import com.techora.domain.cart.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {
    Optional<CartItemEntity> findByCartIdAndProductId(UUID cartId, UUID productId);

    Optional<CartItemEntity> findByIdAndCartUserId(UUID id, UUID userId);
}
