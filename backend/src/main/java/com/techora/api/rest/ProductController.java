package com.techora.api.rest;

import com.techora.app.dto.response.ResponseDto;
import com.techora.app.service.ResponseFactory;
import com.techora.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;
    private final ResponseFactory responseFactory;

    @GetMapping
    public ResponseDto getPublicProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return responseFactory.success(productService.getPublicProducts(categoryId, keyword, page, size));
    }

    @GetMapping("/{slug}")
    public ResponseDto getPublicProduct(@PathVariable String slug) {
        return responseFactory.success(productService.getPublicProduct(slug));
    }
}
