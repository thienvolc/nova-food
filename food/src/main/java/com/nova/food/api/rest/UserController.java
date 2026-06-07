package com.nova.food.api.rest;

import com.nova.food.app.dto.response.ResponseDto;
import com.nova.food.app.service.ResponseFactory;
import com.nova.food.domain.user.dto.request.CreateUserRequest;
import com.nova.food.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final ResponseFactory responseFactory;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseDto create(@Valid @RequestBody CreateUserRequest request) {
        return responseFactory.success(userService.create(request));
    }
}
