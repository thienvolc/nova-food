package com.nova.food.domain.report.dto;

import com.nova.food.domain.order.constant.OrderStatus;

public record OrdersByStatusReportResponse(
        OrderStatus status,
        long orderCount
) {
}
