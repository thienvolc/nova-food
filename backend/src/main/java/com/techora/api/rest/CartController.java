package com.techora.api.rest;

import com.techora.app.dto.response.ResponseDto;
import com.techora.app.service.ResponseFactory;
import com.techora.domain.cart.dto.request.AddCartItemRequest;
import com.techora.domain.cart.dto.request.UpdateCartItemRequest;
import com.techora.domain.cart.service.CartService;
import com.techora.infrastructure.service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService cartService;
    private final ResponseFactory responseFactory;

    @GetMapping
    public ResponseDto getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return responseFactory.success(cartService.getCart(principal.getUserId()));
    }

    @PostMapping("/items")
    public ResponseDto addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return responseFactory.success(cartService.addItem(principal.getUserId(), request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseDto updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return responseFactory.success(cartService.updateItem(principal.getUserId(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseDto removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId
    ) {
        return responseFactory.success(cartService.removeItem(principal.getUserId(), itemId));
    }

    @DeleteMapping
    public ResponseDto clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        return responseFactory.success(cartService.clearCart(principal.getUserId()));
    }
}
