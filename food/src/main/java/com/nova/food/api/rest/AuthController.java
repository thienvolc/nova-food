package com.nova.food.api.rest;

import com.nova.food.app.dto.response.ResponseDto;
import com.nova.food.app.service.ResponseFactory;
import com.nova.food.domain.auth.dto.LoginRequest;
import com.nova.food.domain.auth.dto.RegisterRequest;
import com.nova.food.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@lombok.RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final ResponseFactory responseFactory;

    

    @PostMapping("/register")
    public ResponseDto register(@Valid @RequestBody RegisterRequest request) {
        return responseFactory.success(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseDto login(@Valid @RequestBody LoginRequest request) {
        return responseFactory.success(authService.login(request));
    }
}


