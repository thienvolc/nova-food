package com.techora.domain.cart.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String sku,
        String slug,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}
