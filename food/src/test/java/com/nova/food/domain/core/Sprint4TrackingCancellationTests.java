package com.nova.food.domain.core;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.menu.dto.request.CreateMenuItemRequest;
import com.nova.food.domain.menu.service.MenuItemService;
import com.nova.food.domain.order.constant.OrderStatus;
import com.nova.food.domain.order.dto.request.CancelOrderRequest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class Sprint4TrackingCancellationTests {

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

    @Test
    void orderHasTrackingIdAndStatusHistoryAcrossLifecycle() {
        UUID ownerId = createUser("owner_sprint4_history", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint4_history", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId);

        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        orderService.confirm(order.id(), ownerId, false);
        orderService.markPreparing(order.id(), ownerId, false);
        orderService.markReadyForDelivery(order.id(), ownerId, false);

        var tracking = orderService.track(order.trackingId(), customerId, false, false);
        var history = orderService.listStatusHistory(order.id(), customerId, false, false);

        assertThat(order.trackingId()).startsWith("NF").hasSize(16);
        assertThat(tracking.orderId()).isEqualTo(order.id());
        assertThat(tracking.status()).isEqualTo(OrderStatus.READY_FOR_DELIVERY);
        assertThat(history).extracting("toStatus").containsExactly(
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAID,
                OrderStatus.CONFIRMED,
                OrderStatus.PREPARING,
                OrderStatus.READY_FOR_DELIVERY
        );
        assertThat(history.getFirst().fromStatus()).isNull();
        assertThat(history.get(1).fromStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void customerCanCancelPaidOrderBeforeConfirmation() {
        UUID ownerId = createUser("owner_sprint4_customer_cancel", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint4_customer_cancel", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId);
        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);

        var cancelled = orderService.cancelByCustomer(
                order.id(),
                new CancelOrderRequest("Customer unavailable"),
                customerId
        );
        var history = orderService.listStatusHistory(order.id(), customerId, false, false);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(history.getLast().fromStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(history.getLast().toStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(history.getLast().reason()).isEqualTo("Refund deferred: Customer unavailable");
    }

    @Test
    void restaurantCanCancelBeforeReadyForDelivery() {
        UUID ownerId = createUser("owner_sprint4_owner_cancel", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint4_owner_cancel", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId);
        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        orderService.confirm(order.id(), ownerId, false);

        var cancelled = orderService.cancelByRestaurant(
                order.id(),
                new CancelOrderRequest("Kitchen closed"),
                ownerId,
                false
        );

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancellationAfterReadyForDeliveryIsRejectedWithoutMutation() {
        UUID ownerId = createUser("owner_sprint4_cancel_guard", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint4_cancel_guard", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId);
        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        orderService.confirm(order.id(), ownerId, false);
        orderService.markPreparing(order.id(), ownerId, false);
        orderService.markReadyForDelivery(order.id(), ownerId, false);

        assertThatThrownBy(() -> orderService.cancelByRestaurant(
                order.id(),
                new CancelOrderRequest("Too late"),
                ownerId,
                false
        )).isInstanceOf(BusinessException.class);

        var current = orderService.get(order.id(), customerId, false);
        var history = orderService.listStatusHistory(order.id(), customerId, false, false);

        assertThat(current.status()).isEqualTo(OrderStatus.READY_FOR_DELIVERY);
        assertThat(history).extracting("toStatus").doesNotContain(OrderStatus.CANCELLED);
    }

    @Test
    void nonOwnerCannotTrackRestaurantOrder() {
        UUID ownerId = createUser("owner_sprint4_track_guard", UserRole.RESTAURANT_OWNER);
        UUID anotherOwnerId = createUser("another_owner_sprint4_track_guard", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint4_track_guard", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId);

        assertThatThrownBy(() -> orderService.track(order.trackingId(), anotherOwnerId, false, true))
                .isInstanceOf(BusinessException.class);
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }

    private com.nova.food.domain.order.dto.response.OrderResponse createOrder(UUID ownerId, UUID customerId) {
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Sprint 4 Kitchen " + ownerId,
                null,
                "Sprint 4 Street",
                null
        ), ownerId);
        var menuItem = menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Sprint 4 Dish", null, new BigDecimal("70000.00"), true),
                ownerId,
                false
        );
        return orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 1)
        )), customerId);
    }
}
