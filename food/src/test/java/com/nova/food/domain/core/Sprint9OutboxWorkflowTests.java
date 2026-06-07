package com.nova.food.domain.core;

import com.nova.food.domain.delivery.dto.request.AssignDeliveryRequest;
import com.nova.food.domain.delivery.entity.DeliveryEntity;
import com.nova.food.domain.delivery.repository.DeliveryRepository;
import com.nova.food.domain.delivery.service.DeliveryService;
import com.nova.food.domain.events.outbox.EventOutboxRepository;
import com.nova.food.domain.events.outbox.OutboxPublisherWorker;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class Sprint9OutboxWorkflowTests {

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
    private OutboxPublisherWorker outboxPublisherWorker;

    @Test
    void workflowMutationsAppendExpectedOutboxRecords() {
        UUID ownerId = createUser("owner_sprint9_outbox", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint9_outbox", UserRole.CUSTOMER);
        UUID driverId = createUser("driver_sprint9_outbox", UserRole.DRIVER);
        var order = createOrder(ownerId, customerId);

        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        orderService.confirm(order.id(), ownerId, false);
        orderService.markPreparing(order.id(), ownerId, false);
        orderService.markReadyForDelivery(order.id(), ownerId, false);
        deliveryService.assign(order.id(), new AssignDeliveryRequest(driverId));
        DeliveryEntity delivery = deliveryRepository.findByOrderId(order.id()).orElseThrow();
        deliveryService.start(delivery.getId(), driverId);
        deliveryService.complete(delivery.getId(), driverId);

        var outbox = eventOutboxRepository.findAll();

        assertThat(outbox).extracting("eventType")
                .contains(
                        "payment.completed.v1",
                        "order.ready-for-delivery.v1",
                        "delivery.completed.v1"
                );
        assertThat(outbox).extracting("status").containsOnly(OutboxStatus.PENDING);
    }

    @Test
    void outboxWorkerPublishesPendingEventsAndMarksThemSentInLocalMode() {
        UUID ownerId = createUser("owner_sprint9_worker", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint9_worker", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId);

        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);

        outboxPublisherWorker.publishPending();

        assertThat(eventOutboxRepository.findAll())
                .isNotEmpty()
                .extracting("status")
                .containsOnly(OutboxStatus.SENT);
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }

    private com.nova.food.domain.order.dto.response.OrderResponse createOrder(UUID ownerId, UUID customerId) {
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Sprint 9B Kitchen " + UUID.randomUUID(),
                null,
                "Sprint 9B Street",
                null
        ), ownerId);
        var menuItem = menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Sprint 9B Dish", null, new BigDecimal("85000.00"), true, true, 5, 1),
                ownerId,
                false
        );
        return orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 1)
        )), customerId);
    }
}
