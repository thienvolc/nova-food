package com.nova.food.api.rest;

import com.nova.food.app.dto.response.ResponseDto;
import com.nova.food.app.service.ResponseFactory;
import com.nova.food.domain.order.constant.OrderStatus;
import com.nova.food.domain.report.service.ReportService;
import com.nova.food.infrastructure.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReportController {

    private final ReportService reportService;
    private final ResponseFactory responseFactory;

    @GetMapping("/admin/reports/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseDto adminRevenue(@RequestParam(required = false) UUID restaurantId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {
        return responseFactory.success(reportService.adminRevenue(restaurantId, fromDate, toDate));
    }

    @GetMapping("/admin/reports/orders-by-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseDto adminOrdersByStatus(@RequestParam(required = false) UUID restaurantId,
                                           @RequestParam(required = false) OrderStatus status,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {
        return responseFactory.success(reportService.adminOrdersByStatus(restaurantId, status, fromDate, toDate));
    }

    @GetMapping("/admin/reports/top-menu-items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseDto adminTopMenuItems(@RequestParam(required = false) UUID restaurantId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
                                         @RequestParam(defaultValue = "10") int limit) {
        return responseFactory.success(reportService.adminTopMenuItems(restaurantId, fromDate, toDate, limit));
    }

    @GetMapping("/restaurants/{restaurantId}/reports/revenue")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto restaurantRevenue(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable UUID restaurantId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {
        return responseFactory.success(reportService.restaurantRevenue(
                restaurantId,
                principal.getUserId(),
                principal.hasRole("ADMIN"),
                fromDate,
                toDate
        ));
    }

    @GetMapping("/restaurants/{restaurantId}/reports/orders-by-status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto restaurantOrdersByStatus(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable UUID restaurantId,
                                                @RequestParam(required = false) OrderStatus status,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {
        return responseFactory.success(reportService.restaurantOrdersByStatus(
                restaurantId,
                principal.getUserId(),
                principal.hasRole("ADMIN"),
                status,
                fromDate,
                toDate
        ));
    }

    @GetMapping("/restaurants/{restaurantId}/reports/top-menu-items")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseDto restaurantTopMenuItems(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable UUID restaurantId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
                                              @RequestParam(defaultValue = "10") int limit) {
        return responseFactory.success(reportService.restaurantTopMenuItems(
                restaurantId,
                principal.getUserId(),
                principal.hasRole("ADMIN"),
                fromDate,
                toDate,
                limit
        ));
    }
}
