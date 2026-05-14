package com.techora.domain.product.dto.response;

import com.techora.domain.category.dto.response.CategoryResponse;
import com.techora.domain.product.constant.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        String slug,
        String description,
        BigDecimal price,
        int stockQuantity,
        ProductStatus status,
        CategoryResponse category,
        Instant createdAt,
        Instant updatedAt
) {
}
