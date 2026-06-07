package com.nova.food.domain.core;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.delivery.dto.request.AssignDeliveryRequest;
import com.nova.food.domain.delivery.entity.DeliveryEntity;
import com.nova.food.domain.delivery.repository.DeliveryRepository;
import com.nova.food.domain.delivery.service.DeliveryService;
import com.nova.food.domain.events.outbox.EventOutboxEntity;
import com.nova.food.domain.events.outbox.EventOutboxRepository;
import com.nova.food.domain.events.outbox.OutboxOpsService;
import com.nova.food.domain.events.outbox.OutboxStatus;
import com.nova.food.domain.menu.dto.request.CreateMenuItemRequest;
import com.nova.food.domain.menu.entity.MenuItemEntity;
import com.nova.food.domain.menu.service.MenuItemService;
import com.nova.food.domain.order.dto.request.CreateOrderRequest;
import com.nova.food.domain.order.entity.OrderEntity;
import com.nova.food.domain.order.repository.OrderRepository;
import com.nova.food.domain.order.service.OrderService;
import com.nova.food.domain.payment.dto.request.MockPaymentRequest;
import com.nova.food.domain.payment.repository.PaymentTransactionRepository;
import com.nova.food.domain.payment.service.PaymentService;
import com.nova.food.domain.restaurant.dto.request.CreateRestaurantRequest;
import com.nova.food.domain.restaurant.service.RestaurantService;
import com.nova.food.domain.user.constant.UserRole;
import com.nova.food.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("postgres-it")
@SpringBootTest
@EnabledIfSystemProperty(named = "postgres.it", matches = "true")
class Sprint11PostgresVerificationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private OutboxOpsService outboxOpsService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private EventOutboxRepository eventOutboxRepository;

    @Test
    void criticalWorkflowPersistsCorrectlyOnPostgresql() {
        UUID ownerId = createUser("own_pg_" + shortId(), UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("cus_pg_" + shortId(), UserRole.CUSTOMER);
        UUID driverId = createUser("drv_pg_" + shortId(), UserRole.DRIVER);
        UUID menuItemId = createMenuItem(ownerId);

        CreateOrderRequest firstRequest = new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItemId, 1)
        ));
        var firstOrder = orderService.create(firstRequest, customerId, "pg-order-key");
        var repeatedOrder = orderService.create(firstRequest, customerId, "pg-order-key");

        assertThat(repeatedOrder.id()).isEqualTo(firstOrder.id());
        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItemId, 2)
        )), customerId, "pg-order-key")).isInstanceOf(BusinessException.class);

        var firstPayment = paymentService.payMock(firstOrder.id(), new MockPaymentRequest(true), customerId, "pg-payment-key");
        var repeatedPayment = paymentService.payMock(firstOrder.id(), new MockPaymentRequest(true), customerId, "pg-payment-key");

        assertThat(repeatedPayment.id()).isEqualTo(firstPayment.id());

        orderService.confirm(firstOrder.id(), ownerId, false);
        orderService.markPreparing(firstOrder.id(), ownerId, false);
        orderService.markReadyForDelivery(firstOrder.id(), ownerId, false);

        var firstDelivery = deliveryService.assign(firstOrder.id(), new AssignDeliveryRequest(driverId), "pg-delivery-key");
        var repeatedDelivery = deliveryService.assign(firstOrder.id(), new AssignDeliveryRequest(driverId), "pg-delivery-key");

        assertThat(repeatedDelivery.id()).isEqualTo(firstDelivery.id());

        OrderEntity persistedOrder = orderRepository.findById(firstOrder.id()).orElseThrow();
        MenuItemEntity persistedMenuItem = menuItemService.getRequiredMenuItem(menuItemId);
        DeliveryEntity persistedDelivery = deliveryRepository.findById(firstDelivery.id()).orElseThrow();

        assertThat(persistedOrder.getTrackingId()).startsWith("NF");
        assertThat(persistedOrder.getCreatedAt()).isNotNull();
        assertThat(persistedOrder.getUpdatedAt()).isNotNull();
        assertThat(persistedOrder.getIdempotencyKey()).isEqualTo("pg-order-key");
        assertThat(persistedOrder.getRequestFingerprint()).hasSize(64);
        assertThat(persistedOrder.getStatus().name()).isEqualTo("READY_FOR_DELIVERY");
        assertThat(persistedMenuItem.getStockQuantity()).isEqualTo(4);
        assertThat(persistedDelivery.getAssignedAt()).isNotNull();
        assertThat(persistedDelivery.getStatus().name()).isEqualTo("ASSIGNED");
        assertThat(paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(firstOrder.id())).hasSize(1);
        assertThat(deliveryRepository.findByDriverIdOrderByAssignedAtDesc(driverId)).hasSize(1);

        EventOutboxEntity readyEvent = eventOutboxRepository.findFirstByEventTypeOrderByCreatedAtDesc("order.ready-for-delivery.v1")
                .orElseThrow();
        readyEvent.markFailed("postgres replay verification", Instant.now(), Instant.now());
        eventOutboxRepository.save(readyEvent);

        var replay = outboxOpsService.replayFailed(readyEvent.getId());
        var snapshot = outboxOpsService.snapshot();

        assertThat(replay.status()).isEqualTo(OutboxStatus.SENT);
        assertThat(snapshot.latestPaidOrderTime()).isNotNull();
        assertThat(snapshot.pendingOutboxCount()).isGreaterThanOrEqualTo(0);
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private UUID createMenuItem(UUID ownerId) {
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Sprint 11C Kitchen " + UUID.randomUUID(),
                null,
                "Sprint 11C Street",
                null
        ), ownerId);
        return menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Sprint 11C Dish", null, new BigDecimal("64000.00"), true, true, 5, 1),
                ownerId,
                false
        ).id();
    }
}
