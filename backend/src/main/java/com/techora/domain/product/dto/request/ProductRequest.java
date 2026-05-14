package com.techora.domain.product.dto.request;

import com.techora.domain.product.constant.ProductStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 80) String sku,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @Min(0) int stockQuantity,
        @NotNull UUID categoryId,
        ProductStatus status
) {
}
