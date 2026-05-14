package com.techora.domain.user.mapper;

import com.techora.domain.user.dto.UserSummary;
import com.techora.domain.user.entity.UserEntity;

public final class UserMapper {
    private UserMapper() {}

    public static UserSummary toSummary(UserEntity entity) {
        return new UserSummary(entity.getId(), entity.getUsername(), entity.getRole());
    }
}
