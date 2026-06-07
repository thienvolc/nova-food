package com.nova.food.domain.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopMenuItemReportResponse(
        UUID menuItemId,
        String menuItemName,
        long quantitySold,
        BigDecimal amountTotal
) {
}
