package com.nova.food.domain.order.repository;

import com.nova.food.domain.order.entity.OrderEntity;
import com.nova.food.domain.order.constant.OrderStatus;
import com.nova.food.domain.report.repository.OrdersByStatusReportRow;
import com.nova.food.domain.report.repository.RevenueReportRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Page<OrderEntity> findByCustomerId(UUID customerId, Pageable pageable);

    List<OrderEntity> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);

    Page<OrderEntity> findByRestaurantId(UUID restaurantId, Pageable pageable);

    Optional<OrderEntity> findByTrackingId(String trackingId);

    Optional<OrderEntity> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    boolean existsByTrackingId(String trackingId);

    @Query("""
            select count(o) as completedOrderCount, coalesce(sum(o.total), 0) as paidAmountTotal
            from OrderEntity o
            where (:restaurantId is null or o.restaurantId = :restaurantId)
              and o.status = com.nova.food.domain.order.constant.OrderStatus.COMPLETED
              and o.createdAt >= :fromDate
              and o.createdAt <= :toDate
            """)
    RevenueReportRow revenueReport(UUID restaurantId, Instant fromDate, Instant toDate);

    @Query("""
            select o.status as status, count(o) as orderCount
            from OrderEntity o
            where (:restaurantId is null or o.restaurantId = :restaurantId)
              and (:status is null or o.status = :status)
              and o.createdAt >= :fromDate
              and o.createdAt <= :toDate
            group by o.status
            order by o.status asc
            """)
    List<OrdersByStatusReportRow> ordersByStatus(UUID restaurantId,
                                                 OrderStatus status,
                                                 Instant fromDate,
                                                 Instant toDate);
}
