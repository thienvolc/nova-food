package com.techora.api.rest;

import com.techora.app.dto.response.ResponseDto;
import com.techora.app.service.ResponseFactory;
import com.techora.domain.product.dto.request.ProductRequest;
import com.techora.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {
    private final ProductService productService;
    private final ResponseFactory responseFactory;

    @PostMapping
    public ResponseDto create(@Valid @RequestBody ProductRequest request) {
        return responseFactory.success(productService.create(request));
    }

    @GetMapping("/{productId}")
    public ResponseDto getById(@PathVariable UUID productId) {
        return responseFactory.success(productService.getAdminProduct(productId));
    }

    @GetMapping("/low-stock")
    public ResponseDto getLowStockProducts(
            @RequestParam(required = false) Integer threshold,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return responseFactory.success(productService.getLowStockProducts(threshold, page, size));
    }

    @PutMapping("/{productId}")
    public ResponseDto update(@PathVariable UUID productId, @Valid @RequestBody ProductRequest request) {
        return responseFactory.success(productService.update(productId, request));
    }

    @DeleteMapping("/{productId}")
    public ResponseDto delete(@PathVariable UUID productId) {
        productService.delete(productId);
        return responseFactory.success();
    }
}
