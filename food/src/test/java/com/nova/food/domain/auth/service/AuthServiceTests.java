package com.nova.food.domain.auth.service;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.auth.dto.LoginRequest;
import com.nova.food.domain.auth.dto.RegisterRequest;
import com.nova.food.domain.user.constant.UserRole;
import com.nova.food.domain.user.repository.UserRepository;
import com.nova.food.infrastructure.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthServiceTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void registerCreatesRestaurantOwnerWithHashedPassword() {
        var response = authService.register(new RegisterRequest(
                "owner_auth_test",
                "Password123!",
                UserRole.RESTAURANT_OWNER
        ));

        var user = userRepository.findByUsername("owner_auth_test").orElseThrow();
        Claims claims = jwtService.parse(response.accessToken());

        assertThat(user.getRole()).isEqualTo(UserRole.RESTAURANT_OWNER);
        assertThat(user.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(passwordEncoder.matches("Password123!", user.getPasswordHash())).isTrue();
        assertThat(claims.get("username", String.class)).isEqualTo("owner_auth_test");
        assertThat(claims.get("role", String.class)).isEqualTo("RESTAURANT_OWNER");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        authService.register(new RegisterRequest("duplicate_auth_test", "Password123!", UserRole.CUSTOMER));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("duplicate_auth_test", "Password123!", UserRole.CUSTOMER)
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void registerRejectsAdminRole() {
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("admin_auth_test", "Password123!", UserRole.ADMIN)
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        authService.register(new RegisterRequest("wrong_password_test", "Password123!", UserRole.CUSTOMER));

        assertThatThrownBy(() -> authService.login(new LoginRequest("wrong_password_test", "wrong-password")))
                .isInstanceOf(BusinessException.class);
    }
}
