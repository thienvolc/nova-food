package com.techora.domain.user.dto;

import com.techora.domain.user.entity.UserRole;

import java.util.UUID;

public record UserSummary(UUID id, String username, UserRole role) {
}
