package com.techora.domain.auth.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.auth.dto.AuthResponse;
import com.techora.domain.auth.dto.LoginRequest;
import com.techora.domain.auth.dto.RegisterRequest;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.user.entity.UserEntity;
import com.techora.domain.user.repository.UserRepository;
import com.techora.domain.user.service.UserService;
import com.techora.infrastructure.service.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@lombok.RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    @Transactional

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ResponseCode.USER_ALREADY_EXISTS);
        }
        UserEntity user = userService.createUser(request.username(), request.password());
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name()));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ResponseCode.BAD_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ResponseCode.BAD_CREDENTIALS);
        }
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name()));
    }
}

