package com.nova.food.domain.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID menuItemId,
        String menuItemName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
