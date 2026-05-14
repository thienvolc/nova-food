package com.techora.domain.order.mapper;

import com.techora.domain.order.dto.response.AdminOrderEventResponse;
import com.techora.domain.order.dto.response.OrderEventResponse;
import com.techora.domain.order.entity.OrderEventEntity;
import org.springframework.stereotype.Service;

@Service
public class OrderEventMapper {
    public OrderEventResponse toResponse(OrderEventEntity event) {
        return new OrderEventResponse(
                event.getId(),
                event.getOrder().getId(),
                event.getEventType(),
                event.getOldStatus(),
                event.getNewStatus(),
                event.getReason(),
                event.getCreatedAt()
        );
    }

    public AdminOrderEventResponse toAdminResponse(OrderEventEntity event) {
        return new AdminOrderEventResponse(
                event.getId(),
                event.getOrder().getId(),
                event.getEventType(),
                event.getOldStatus(),
                event.getNewStatus(),
                event.getReason(),
                event.getMetadata(),
                event.getActorType(),
                event.getActorId(),
                event.getActorName(),
                event.getCreatedAt()
        );
    }
}
