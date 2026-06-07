package com.nova.food.domain.user.repository;

import com.nova.food.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByUsername(String username);

    Optional<UserEntity> findByUsername(String username);
}
