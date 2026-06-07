package com.nova.food.domain.user.dto.response;

import com.nova.food.domain.user.constant.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        UserRole role,
        Instant createdAt
) {
}
