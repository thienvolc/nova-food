package com.techora.domain.order.mapper;

import com.techora.domain.order.dto.response.OrderItemResponse;
import com.techora.domain.order.dto.response.OrderResponse;
import com.techora.domain.order.entity.OrderEntity;
import com.techora.domain.order.entity.OrderItemEntity;
import org.springframework.stereotype.Service;

@Service
public class OrderMapper {
    public OrderResponse toResponse(OrderEntity order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getStatus(),
                order.getTotal(),
                order.getItems().stream().map(this::toItemResponse).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItemEntity item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getSku(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
