package com.nova.food.domain.auth.service;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.auth.dto.AuthResponse;
import com.nova.food.domain.auth.dto.LoginRequest;
import com.nova.food.domain.auth.dto.RegisterRequest;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.user.constant.UserRole;
import com.nova.food.domain.user.entity.UserEntity;
import com.nova.food.domain.user.repository.UserRepository;
import com.nova.food.domain.user.service.UserService;
import com.nova.food.infrastructure.service.JwtService;
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
        UserRole role = resolvePublicRegistrationRole(request.role());
        UserEntity user = userService.createUser(request.username(), request.password(), role);
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

    private UserRole resolvePublicRegistrationRole(UserRole requestedRole) {
        if (requestedRole == null) {
            return UserRole.CUSTOMER;
        }
        if (requestedRole == UserRole.CUSTOMER || requestedRole == UserRole.RESTAURANT_OWNER) {
            return requestedRole;
        }
        throw new BusinessException(ResponseCode.INVALID_USER_ROLE);
    }
}
