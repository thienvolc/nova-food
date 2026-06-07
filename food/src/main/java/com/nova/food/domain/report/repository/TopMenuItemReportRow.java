package com.nova.food.domain.report.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface TopMenuItemReportRow {

    UUID getMenuItemId();

    String getMenuItemName();

    long getQuantitySold();

    BigDecimal getAmountTotal();
}
