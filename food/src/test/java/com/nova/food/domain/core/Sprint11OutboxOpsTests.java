package com.nova.food.domain.core;

import com.nova.food.domain.delivery.dto.request.AssignDeliveryRequest;
import com.nova.food.domain.delivery.entity.DeliveryEntity;
import com.nova.food.domain.delivery.repository.DeliveryRepository;
import com.nova.food.domain.delivery.service.DeliveryService;
import com.nova.food.domain.events.outbox.EventOutboxEntity;
import com.nova.food.domain.events.outbox.EventOutboxRepository;
import com.nova.food.domain.events.outbox.OutboxOpsService;
import com.nova.food.domain.events.outbox.OutboxStatus;
import com.nova.food.domain.menu.dto.request.CreateMenuItemRequest;
import com.nova.food.domain.menu.service.MenuItemService;
import com.nova.food.domain.order.dto.request.CreateOrderRequest;
import com.nova.food.domain.order.service.OrderService;
import com.nova.food.domain.payment.dto.request.MockPaymentRequest;
import com.nova.food.domain.payment.service.PaymentService;
import com.nova.food.domain.restaurant.dto.request.CreateRestaurantRequest;
import com.nova.food.domain.restaurant.service.RestaurantService;
import com.nova.food.domain.user.constant.UserRole;
import com.nova.food.domain.user.service.UserService;
import com.nova.food.infrastructure.kafka.producer.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class Sprint11OutboxOpsTests {

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
    private DeliveryRepository deliveryRepository;

    @Autowired
    private EventOutboxRepository eventOutboxRepository;

    @Autowired
    private OutboxOpsService outboxOpsService;

    @MockitoSpyBean
    private DomainEventPublisher domainEventPublisher;

    @Test
    void snapshotExposesWorkflowMetricsFromOutboxState() {
        UUID ownerId = createUser("owner_sprint11_outbox_snapshot", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint11_outbox_snapshot", UserRole.CUSTOMER);
        UUID driverId = createUser("driver_sprint11_outbox_snapshot", UserRole.DRIVER);
        var order = createOrder(ownerId, customerId);

        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        orderService.confirm(order.id(), ownerId, false);
        orderService.markPreparing(order.id(), ownerId, false);
        orderService.markReadyForDelivery(order.id(), ownerId, false);
        deliveryService.assign(order.id(), new AssignDeliveryRequest(driverId));
        DeliveryEntity delivery = deliveryRepository.findByOrderId(order.id()).orElseThrow();
        deliveryService.start(delivery.getId(), driverId);
        deliveryService.complete(delivery.getId(), driverId);

        EventOutboxEntity paymentOutbox = findEvent("payment.completed.v1");
        EventOutboxEntity deliveryOutbox = findEvent("delivery.completed.v1");
        paymentOutbox.markFailed("payment publish failed", Instant.now(), Instant.now());

        var snapshot = outboxOpsService.snapshot();
        var failed = outboxOpsService.listFailed();

        assertThat(snapshot.pendingOutboxCount()).isGreaterThanOrEqualTo(2);
        assertThat(snapshot.failedOutboxCount()).isEqualTo(1);
        assertThat(snapshot.latestPaidOrderTime()).isNotNull();
        assertThat(snapshot.latestCompletedDeliveryTime()).isNotNull();
        assertThat(failed).extracting("eventType").contains("payment.completed.v1");
        assertThat(failed).extracting("id").contains(paymentOutbox.getId());
        assertThat(deliveryOutbox.getCreatedAt()).isNotNull();
    }

    @Test
    void replayFailedMarksEventSentAfterImmediateRetry() {
        UUID ownerId = createUser("owner_sprint11_outbox_replay", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint11_outbox_replay", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId);

        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);

        EventOutboxEntity outbox = findEvent("payment.completed.v1");
        outbox.markFailed("temporary error", Instant.now(), Instant.now());

        var replay = outboxOpsService.replayFailed(outbox.getId());

        assertThat(replay.status()).isEqualTo(OutboxStatus.SENT);
        assertThat(replay.lastError()).isNull();
    }

    @Test
    void replayFailedKeepsEventFailedWhenPublisherStillThrows() {
        UUID ownerId = createUser("owner_sprint11_outbox_still_fails", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint11_outbox_still_fails", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId);

        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        EventOutboxEntity outbox = findEvent("payment.completed.v1");
        outbox.markFailed("first failure", Instant.now(), Instant.now());

        doThrow(new IllegalStateException("broker unavailable"))
                .when(domainEventPublisher)
                .publish(anyString(), anyString(), anyString());

        var replay = outboxOpsService.replayFailed(outbox.getId());

        assertThat(replay.status()).isEqualTo(OutboxStatus.FAILED);
        assertThat(replay.lastError()).contains("broker unavailable");
        Mockito.reset(domainEventPublisher);
    }

    private EventOutboxEntity findEvent(String eventType) {
        return eventOutboxRepository.findFirstByEventTypeOrderByCreatedAtDesc(eventType).orElseThrow();
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }

    private com.nova.food.domain.order.dto.response.OrderResponse createOrder(UUID ownerId, UUID customerId) {
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Sprint 11B Kitchen " + UUID.randomUUID(),
                null,
                "Sprint 11B Street",
                null
        ), ownerId);
        var menuItem = menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Sprint 11B Dish", null, new BigDecimal("93000.00"), true, true, 5, 1),
                ownerId,
                false
        );
        return orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 1)
        )), customerId);
    }
}
