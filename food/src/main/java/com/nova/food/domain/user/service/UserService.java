package com.nova.food.domain.user.service;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.user.constant.UserRole;
import com.nova.food.domain.user.dto.request.CreateUserRequest;
import com.nova.food.domain.user.dto.response.UserResponse;
import com.nova.food.domain.user.entity.UserEntity;
import com.nova.food.domain.user.mapper.UserMapper;
import com.nova.food.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ResponseCode.USER_ALREADY_EXISTS);
        }
        return userMapper.toResponse(createUser(request.username(), request.password(), request.role()));
    }

    @Transactional
    public UserEntity createUser(String username, String password, UserRole role) {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .createdAt(Instant.now())
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserEntity getRequiredUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.USERNAME_NOT_FOUND));
    }

    public void validateRole(UserEntity user, UserRole role) {
        if (user.getRole() != role) {
            throw new BusinessException(ResponseCode.INVALID_USER_ROLE);
        }
    }
}
