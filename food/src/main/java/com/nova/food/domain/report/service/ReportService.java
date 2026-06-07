package com.nova.food.domain.report.service;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.order.constant.OrderStatus;
import com.nova.food.domain.order.repository.OrderItemRepository;
import com.nova.food.domain.order.repository.OrderRepository;
import com.nova.food.domain.report.dto.OrdersByStatusReportResponse;
import com.nova.food.domain.report.dto.RevenueReportResponse;
import com.nova.food.domain.report.dto.TopMenuItemReportResponse;
import com.nova.food.domain.restaurant.entity.RestaurantEntity;
import com.nova.food.domain.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int MAX_TOP_ITEMS = 20;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantService restaurantService;

    @Transactional(readOnly = true)
    public RevenueReportResponse adminRevenue(UUID restaurantId, Instant fromDate, Instant toDate) {
        validateRange(fromDate, toDate);
        if (restaurantId != null) {
            restaurantService.getRequiredRestaurant(restaurantId);
        }
        return revenue(restaurantId, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse restaurantRevenue(UUID restaurantId, UUID ownerId, boolean admin, Instant fromDate, Instant toDate) {
        validateCanViewRestaurantReports(restaurantId, ownerId, admin);
        validateRange(fromDate, toDate);
        return revenue(restaurantId, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public List<OrdersByStatusReportResponse> adminOrdersByStatus(UUID restaurantId,
                                                                  OrderStatus status,
                                                                  Instant fromDate,
                                                                  Instant toDate) {
        validateRange(fromDate, toDate);
        if (restaurantId != null) {
            restaurantService.getRequiredRestaurant(restaurantId);
        }
        return ordersByStatus(restaurantId, status, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public List<OrdersByStatusReportResponse> restaurantOrdersByStatus(UUID restaurantId,
                                                                       UUID ownerId,
                                                                       boolean admin,
                                                                       OrderStatus status,
                                                                       Instant fromDate,
                                                                       Instant toDate) {
        validateCanViewRestaurantReports(restaurantId, ownerId, admin);
        validateRange(fromDate, toDate);
        return ordersByStatus(restaurantId, status, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public List<TopMenuItemReportResponse> adminTopMenuItems(UUID restaurantId,
                                                            Instant fromDate,
                                                            Instant toDate,
                                                            int limit) {
        validateRange(fromDate, toDate);
        if (restaurantId != null) {
            restaurantService.getRequiredRestaurant(restaurantId);
        }
        return topMenuItems(restaurantId, fromDate, toDate, limit);
    }

    @Transactional(readOnly = true)
    public List<TopMenuItemReportResponse> restaurantTopMenuItems(UUID restaurantId,
                                                                 UUID ownerId,
                                                                 boolean admin,
                                                                 Instant fromDate,
                                                                 Instant toDate,
                                                                 int limit) {
        validateCanViewRestaurantReports(restaurantId, ownerId, admin);
        validateRange(fromDate, toDate);
        return topMenuItems(restaurantId, fromDate, toDate, limit);
    }

    private RevenueReportResponse revenue(UUID restaurantId, Instant fromDate, Instant toDate) {
        var row = orderRepository.revenueReport(restaurantId, fromDate, toDate);
        BigDecimal total = row.getPaidAmountTotal() == null ? BigDecimal.ZERO : row.getPaidAmountTotal();
        return new RevenueReportResponse(restaurantId, fromDate, toDate, row.getCompletedOrderCount(), total);
    }

    private List<OrdersByStatusReportResponse> ordersByStatus(UUID restaurantId,
                                                              OrderStatus status,
                                                              Instant fromDate,
                                                              Instant toDate) {
        return orderRepository.ordersByStatus(restaurantId, status, fromDate, toDate).stream()
                .map(row -> new OrdersByStatusReportResponse(row.getStatus(), row.getOrderCount()))
                .toList();
    }

    private List<TopMenuItemReportResponse> topMenuItems(UUID restaurantId, Instant fromDate, Instant toDate, int limit) {
        validateLimit(limit);
        return orderItemRepository.topMenuItems(restaurantId, fromDate, toDate, limit).stream()
                .map(row -> new TopMenuItemReportResponse(
                        row.getMenuItemId(),
                        row.getMenuItemName(),
                        row.getQuantitySold(),
                        row.getAmountTotal()
                ))
                .toList();
    }

    private void validateCanViewRestaurantReports(UUID restaurantId, UUID ownerId, boolean admin) {
        RestaurantEntity restaurant = restaurantService.getRequiredRestaurant(restaurantId);
        restaurantService.validateCanManage(restaurant, ownerId, admin);
    }

    private void validateRange(Instant fromDate, Instant toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new BusinessException(ResponseCode.INVALID_REPORT_DATE_RANGE);
        }
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_TOP_ITEMS) {
            throw new BusinessException(ResponseCode.INVALID_PAGE_REQUEST);
        }
    }
}
