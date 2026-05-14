package com.techora.domain.user.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.user.dto.UserSummary;
import com.techora.domain.user.entity.UserEntity;
import com.techora.domain.user.entity.UserRole;
import com.techora.domain.user.mapper.UserMapper;
import com.techora.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserEntity createUser(String username, String rawPassword) {
        return createUser(username, rawPassword, UserRole.USER);
    }

    @Transactional
    public UserEntity createUser(String username, String rawPassword, UserRole role) {
        return userRepository.save(UserEntity.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .createdAt(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public UserSummary getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .map(UserMapper::toSummary)
                .orElseThrow(() -> new BusinessException(ResponseCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserEntity getRequiredEntity(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.USER_NOT_FOUND));
    }
}
