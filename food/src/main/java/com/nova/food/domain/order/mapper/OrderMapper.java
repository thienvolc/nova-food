package com.nova.food.domain.order.mapper;

import com.nova.food.domain.order.dto.response.OrderItemResponse;
import com.nova.food.domain.order.dto.response.OrderResponse;
import com.nova.food.domain.order.dto.response.OrderStatusHistoryResponse;
import com.nova.food.domain.order.dto.response.OrderTrackingResponse;
import com.nova.food.domain.order.entity.OrderEntity;
import com.nova.food.domain.order.entity.OrderItemEntity;
import com.nova.food.domain.order.entity.OrderStatusHistoryEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderMapper {

    public OrderResponse toResponse(OrderEntity order, List<OrderItemEntity> items) {
        return new OrderResponse(
                order.getId(),
                order.getTrackingId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getTotal(),
                items.stream().map(this::toItemResponse).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public OrderTrackingResponse toTrackingResponse(OrderEntity order) {
        return new OrderTrackingResponse(
                order.getTrackingId(),
                order.getId(),
                order.getStatus(),
                order.getRestaurantId(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistoryEntity history) {
        return new OrderStatusHistoryResponse(
                history.getOrderId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedByUserId(),
                history.getReason(),
                history.getChangedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItemEntity item) {
        return new OrderItemResponse(
                item.getId(),
                item.getMenuItemId(),
                item.getMenuItemName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal()
        );
    }
}
