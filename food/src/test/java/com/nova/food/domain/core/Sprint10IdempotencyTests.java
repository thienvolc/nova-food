package com.nova.food.domain.core;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.delivery.dto.request.AssignDeliveryRequest;
import com.nova.food.domain.delivery.repository.DeliveryRepository;
import com.nova.food.domain.delivery.service.DeliveryService;
import com.nova.food.domain.menu.dto.request.CreateMenuItemRequest;
import com.nova.food.domain.menu.service.MenuItemService;
import com.nova.food.domain.order.dto.request.CreateOrderRequest;
import com.nova.food.domain.order.service.OrderService;
import com.nova.food.domain.payment.repository.PaymentTransactionRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class Sprint10IdempotencyTests {

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
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Test
    void repeatedPaymentWithSameIdempotencyKeyReturnsSameTransaction() {
        UUID ownerId = createUser("owner_sprint10_pay", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint10_pay", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId);

        var first = paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId, "pay-key-1");
        var second = paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId, "pay-key-1");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(order.id())).hasSize(1);
    }

    @Test
    void repeatedDeliveryAssignmentWithSameIdempotencyKeyReturnsSameDelivery() {
        UUID ownerId = createUser("owner_sprint10_delivery", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint10_delivery", UserRole.CUSTOMER);
        UUID driverId = createUser("driver_sprint10_delivery", UserRole.DRIVER);
        var order = readyOrder(ownerId, customerId);

        var first = deliveryService.assign(order.id(), new AssignDeliveryRequest(driverId), "delivery-key-1");
        var second = deliveryService.assign(order.id(), new AssignDeliveryRequest(driverId), "delivery-key-1");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(deliveryRepository.findByDriverIdOrderByAssignedAtDesc(driverId)).hasSize(1);
    }

    @Test
    void repeatedPaymentWithoutIdempotencyKeyStillFailsAfterStatusChange() {
        UUID ownerId = createUser("owner_sprint10_no_key", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint10_no_key", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId);

        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId, null);

        assertThatThrownBy(() -> paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId, null))
                .isInstanceOf(BusinessException.class);
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }

    private com.nova.food.domain.order.dto.response.OrderResponse createOrder(UUID ownerId, UUID customerId) {
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Sprint 10 Kitchen " + UUID.randomUUID(),
                null,
                "Sprint 10 Street",
                null
        ), ownerId);
        var menuItem = menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Sprint 10 Dish", null, new BigDecimal("77000.00"), true, true, 5, 1),
                ownerId,
                false
        );
        return orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 1)
        )), customerId);
    }

    private com.nova.food.domain.order.dto.response.OrderResponse readyOrder(UUID ownerId, UUID customerId) {
        var order = createOrder(ownerId, customerId);
        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId, "prep-payment-key");
        orderService.confirm(order.id(), ownerId, false);
        orderService.markPreparing(order.id(), ownerId, false);
        return orderService.markReadyForDelivery(order.id(), ownerId, false);
    }
}
