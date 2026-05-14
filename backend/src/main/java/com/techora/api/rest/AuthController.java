package com.techora.api.rest;

import com.techora.app.dto.response.ResponseDto;
import com.techora.app.service.ResponseFactory;
import com.techora.domain.auth.dto.LoginRequest;
import com.techora.domain.auth.dto.RegisterRequest;
import com.techora.domain.auth.service.AuthService;
import com.techora.domain.user.service.UserService;
import com.techora.infrastructure.service.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@lombok.RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final ResponseFactory responseFactory;

    @PostMapping("/register")
    public ResponseDto register(@Valid @RequestBody RegisterRequest request) {
        return responseFactory.success(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseDto login(@Valid @RequestBody LoginRequest request) {
        return responseFactory.success(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseDto me(@AuthenticationPrincipal UserPrincipal principal) {
        return responseFactory.success(userService.getCurrentUser(principal.getUserId()));
    }
}

