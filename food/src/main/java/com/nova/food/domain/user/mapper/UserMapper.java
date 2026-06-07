package com.nova.food.domain.user.mapper;

import com.nova.food.domain.user.dto.response.UserResponse;
import com.nova.food.domain.user.entity.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class UserMapper {

    public UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
