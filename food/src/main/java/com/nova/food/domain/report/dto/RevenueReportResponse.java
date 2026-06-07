package com.nova.food.domain.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RevenueReportResponse(
        UUID restaurantId,
        Instant fromDate,
        Instant toDate,
        long completedOrderCount,
        BigDecimal paidAmountTotal
) {
}
