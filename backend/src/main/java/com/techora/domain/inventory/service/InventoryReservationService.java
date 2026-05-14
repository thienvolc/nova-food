package com.techora.domain.inventory.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.inventory.constant.InventoryReservationStatus;
import com.techora.domain.inventory.entity.InventoryReservationEntity;
import com.techora.domain.inventory.repository.InventoryReservationRepository;
import com.techora.domain.order.entity.OrderEntity;
import com.techora.domain.order.entity.OrderItemEntity;
import com.techora.domain.outbox.service.OutboxEventService;
import com.techora.domain.product.entity.ProductEntity;
import com.techora.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryReservationService {
    private static final int RESERVATION_EXPIRATION_MINUTES = 30;

    private final InventoryReservationRepository inventoryReservationRepository;
    private final ProductService productService;
    private final OutboxEventService outboxEventService;

    public void reserve(OrderEntity order) {
        order.getItems().forEach(item -> reserve(order, item));
    }

    public void confirm(OrderEntity order) {
        List<InventoryReservationEntity> reservations = inventoryReservationRepository.findLockedByOrderId(order.getId());
        reservations.stream()
                .filter(this::isReserved)
                .forEach(this::confirm);
    }

    public void release(OrderEntity order) {
        List<InventoryReservationEntity> reservations = inventoryReservationRepository.findLockedByOrderId(order.getId());
        reservations.stream()
                .filter(this::isReserved)
                .forEach(reservation -> updateStatus(reservation, InventoryReservationStatus.RELEASED));
    }

    public int expireAbandonedReservations() {
        List<InventoryReservationEntity> expiredReservations =
                inventoryReservationRepository.findByStatusAndExpiresAtBefore(
                        InventoryReservationStatus.RESERVED,
                        Instant.now()
                );
        expiredReservations.forEach(reservation -> updateStatus(reservation, InventoryReservationStatus.EXPIRED));
        return expiredReservations.size();
    }

    private void reserve(OrderEntity order, OrderItemEntity item) {
        ProductEntity product = productService.getRequiredActiveEntityForUpdate(item.getProductId());
        validateAvailableQuantity(product, item.getQuantity());
        inventoryReservationRepository.save(buildReservation(order, product, item.getQuantity()));
    }

    private void confirm(InventoryReservationEntity reservation) {
        ProductEntity product = productService.reduceStockAndReturn(
                reservation.getProduct(),
                reservation.getQuantity()
        );
        updateStatus(reservation, InventoryReservationStatus.CONFIRMED);
        outboxEventService.recordStockReduced(product, reservation.getQuantity());
    }

    private void validateAvailableQuantity(ProductEntity product, int requestedQuantity) {
        int reservedQuantity = inventoryReservationRepository.sumQuantityByProductIdAndStatus(
                product.getId(),
                InventoryReservationStatus.RESERVED
        );
        if (product.getStockQuantity() - reservedQuantity < requestedQuantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
    }

    private InventoryReservationEntity buildReservation(OrderEntity order, ProductEntity product, int quantity) {
        Instant now = Instant.now();
        return InventoryReservationEntity.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .status(InventoryReservationStatus.RESERVED)
                .expiresAt(now.plus(RESERVATION_EXPIRATION_MINUTES, ChronoUnit.MINUTES))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private boolean isReserved(InventoryReservationEntity reservation) {
        return reservation.getStatus() == InventoryReservationStatus.RESERVED;
    }

    private void updateStatus(
            InventoryReservationEntity reservation,
            InventoryReservationStatus status
    ) {
        reservation.setStatus(status);
        reservation.setUpdatedAt(Instant.now());
        inventoryReservationRepository.save(reservation);
    }
}
