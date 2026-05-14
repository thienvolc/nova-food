package com.techora.domain.order.dto.request;

import com.techora.domain.order.constant.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status
) {
}
