package com.techora.domain.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techora.domain.order.constant.OrderEventActorType;
import com.techora.domain.order.constant.OrderEventType;
import com.techora.domain.order.constant.OrderStatus;
import com.techora.domain.order.dto.response.AdminOrderEventResponse;
import com.techora.domain.order.dto.response.OrderEventResponse;
import com.techora.domain.order.entity.OrderEntity;
import com.techora.domain.order.entity.OrderEventEntity;
import com.techora.domain.order.mapper.OrderEventMapper;
import com.techora.domain.order.repository.OrderEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderEventService {
    private static final String CHECKOUT_COMPLETED = "checkout_completed";
    private static final String USER_PAYMENT_UPDATE = "user_payment_update";
    private static final String ADMIN_STATUS_UPDATE = "admin_status_update";
    private static final String SYSTEM_STATUS_UPDATE = "system_status_update";
    private static final String EMPTY_METADATA = "{}";

    private final OrderEventRepository orderEventRepository;
    private final OrderEventMapper orderEventMapper;
    private final ObjectMapper objectMapper;

    public void recordOrderPlaced(OrderEntity order) {
        save(
                order,
                OrderEventType.ORDER_PLACED,
                null,
                order.getStatus(),
                CHECKOUT_COMPLETED,
                OrderEventActorType.USER,
                order.getUser().getId(),
                order.getUser().getUsername(),
                Map.of()
        );
    }

    public void recordAdminStatusChanged(
            OrderEntity order,
            OrderStatus oldStatus,
            OrderStatus newStatus,
            UUID actorId,
            String actorName
    ) {
        save(
                order,
                eventTypeFor(newStatus),
                oldStatus,
                newStatus,
                ADMIN_STATUS_UPDATE,
                OrderEventActorType.ADMIN,
                actorId,
                actorName,
                Map.of()
        );
    }

    public void recordUserStatusChanged(
            OrderEntity order,
            OrderStatus oldStatus,
            OrderStatus newStatus,
            UUID actorId,
            String actorName
    ) {
        save(
                order,
                eventTypeFor(newStatus),
                oldStatus,
                newStatus,
                USER_PAYMENT_UPDATE,
                OrderEventActorType.USER,
                actorId,
                actorName,
                Map.of()
        );
    }

    public void recordSystemStatusChanged(OrderEntity order, OrderStatus oldStatus, OrderStatus newStatus) {
        save(
                order,
                eventTypeFor(newStatus),
                oldStatus,
                newStatus,
                SYSTEM_STATUS_UPDATE,
                OrderEventActorType.SYSTEM,
                null,
                null,
                Map.of()
        );
    }

    public List<OrderEventResponse> getUserEvents(UUID orderId) {
        return orderEventRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(orderEventMapper::toResponse)
                .toList();
    }

    public List<AdminOrderEventResponse> getAdminEvents(UUID orderId) {
        return orderEventRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(orderEventMapper::toAdminResponse)
                .toList();
    }

    private OrderEventType eventTypeFor(OrderStatus newStatus) {
        if (newStatus == OrderStatus.CANCELLED) {
            return OrderEventType.ORDER_CANCELLED;
        }
        if (newStatus == OrderStatus.PAID) {
            return OrderEventType.PAYMENT_CONFIRMED;
        }
        if (newStatus == OrderStatus.PAYMENT_FAILED) {
            return OrderEventType.PAYMENT_FAILED;
        }
        return OrderEventType.ORDER_STATUS_CHANGED;
    }

    private void save(
            OrderEntity order,
            OrderEventType eventType,
            OrderStatus oldStatus,
            OrderStatus newStatus,
            String reason,
            OrderEventActorType actorType,
            UUID actorId,
            String actorName,
            Map<String, Object> metadata
    ) {
        orderEventRepository.save(OrderEventEntity.builder()
                .order(order)
                .eventType(eventType)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .metadata(toJson(metadata))
                .actorType(actorType)
                .actorId(actorId)
                .actorName(actorName)
                .createdAt(Instant.now())
                .build());
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata.isEmpty()) {
            return EMPTY_METADATA;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize order event metadata", ex);
        }
    }
}
