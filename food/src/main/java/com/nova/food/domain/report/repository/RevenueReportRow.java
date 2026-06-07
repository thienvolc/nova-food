package com.nova.food.domain.report.repository;

import java.math.BigDecimal;

public interface RevenueReportRow {

    long getCompletedOrderCount();

    BigDecimal getPaidAmountTotal();
}
