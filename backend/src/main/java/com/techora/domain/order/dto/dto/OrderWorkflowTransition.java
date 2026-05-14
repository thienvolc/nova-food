package com.techora.domain.order.dto.dto;

import com.techora.domain.order.constant.OrderStatus;
import com.techora.domain.order.entity.OrderEntity;

public record OrderWorkflowTransition(
        OrderEntity order,
        OrderStatus oldStatus,
        OrderStatus newStatus
) {
}
