package com.nova.food.domain.order.repository;

import com.nova.food.domain.order.entity.OrderItemEntity;
import com.nova.food.domain.report.repository.TopMenuItemReportRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, UUID> {

    List<OrderItemEntity> findByOrderId(UUID orderId);

    @Query("""
            select i.menuItemId as menuItemId,
                   min(i.menuItemName) as menuItemName,
                   sum(i.quantity) as quantitySold,
                   sum(i.lineTotal) as amountTotal
            from OrderItemEntity i
            join OrderEntity o on o.id = i.orderId
            where (:restaurantId is null or o.restaurantId = :restaurantId)
              and o.status = com.nova.food.domain.order.constant.OrderStatus.COMPLETED
              and o.createdAt >= :fromDate
              and o.createdAt <= :toDate
            group by i.menuItemId
            order by sum(i.quantity) desc, sum(i.lineTotal) desc
            limit :limit
            """)
    List<TopMenuItemReportRow> topMenuItems(UUID restaurantId, Instant fromDate, Instant toDate, int limit);
}
