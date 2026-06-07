package com.nova.food.domain.delivery.mapper;

import com.nova.food.domain.delivery.dto.response.DeliveryResponse;
import com.nova.food.domain.delivery.entity.DeliveryEntity;
import org.springframework.stereotype.Service;

@Service
public class DeliveryMapper {

    public DeliveryResponse toResponse(DeliveryEntity delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getDriverId(),
                delivery.getStatus(),
                delivery.getAssignedAt(),
                delivery.getStartedAt(),
                delivery.getCompletedAt(),
                delivery.getUpdatedAt()
        );
    }
}
