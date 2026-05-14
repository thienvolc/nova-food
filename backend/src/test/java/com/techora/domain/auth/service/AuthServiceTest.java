package com.techora.domain.auth.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.auth.dto.LoginRequest;
import com.techora.domain.auth.dto.RegisterRequest;
import com.techora.domain.common.constant.ResponseCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {
    private static final String PASSWORD = "password";

    @Autowired
    private AuthService authService;

    @Test
    void registerCreatesUserAndReturnsToken() {
        assertThat(authService.register(new RegisterRequest("auth-owner-a", PASSWORD)).accessToken())
                .isNotBlank();
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        authService.register(new RegisterRequest("auth-owner-b", PASSWORD));

        assertThat(authService.login(new LoginRequest("auth-owner-b", PASSWORD)).accessToken())
                .isNotBlank();
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("auth-owner-c", PASSWORD);
        authService.register(request);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.USER_ALREADY_EXISTS.getDefaultMessage());
    }
}
