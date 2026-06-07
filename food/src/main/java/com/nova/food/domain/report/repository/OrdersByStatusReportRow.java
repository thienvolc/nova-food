package com.nova.food.domain.report.repository;

import com.nova.food.domain.order.constant.OrderStatus;

public interface OrdersByStatusReportRow {

    OrderStatus getStatus();

    long getOrderCount();
}
