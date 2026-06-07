package com.nova.food.domain.core;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.delivery.constant.DeliveryStatus;
import com.nova.food.domain.delivery.dto.request.AssignDeliveryRequest;
import com.nova.food.domain.delivery.service.DeliveryService;
import com.nova.food.domain.menu.dto.request.CreateMenuItemRequest;
import com.nova.food.domain.menu.service.MenuItemService;
import com.nova.food.domain.order.constant.OrderStatus;
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
class Sprint2LifecycleDeliveryTests {

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

    @Test
    void ownerProgressesPaidOrderThenAdminAssignsDriverAndDriverCompletesDelivery() {
        UUID ownerId = createUser("owner_sprint2_flow", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint2_flow", UserRole.CUSTOMER);
        UUID driverId = createAvailableDriver("driver_sprint2_flow");
        var order = createPaidOrder(ownerId, customerId);

        var confirmed = orderService.confirm(order.id(), ownerId, false);
        var preparing = orderService.markPreparing(order.id(), ownerId, false);
        var ready = orderService.markReadyForDelivery(order.id(), ownerId, false);
        var delivery = deliveryService.assign(order.id(), new AssignDeliveryRequest(driverId));
        var delivering = deliveryService.start(delivery.id(), driverId);
        var completed = deliveryService.complete(delivery.id(), driverId);
        var completedOrder = orderService.get(order.id(), customerId, false);

        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(preparing.status()).isEqualTo(OrderStatus.PREPARING);
        assertThat(ready.status()).isEqualTo(OrderStatus.READY_FOR_DELIVERY);
        assertThat(delivery.status()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(delivering.status()).isEqualTo(DeliveryStatus.DELIVERING);
        assertThat(completed.status()).isEqualTo(DeliveryStatus.COMPLETED);
        assertThat(completedOrder.status()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void nonOwnerCannotMutateAnotherRestaurantOrder() {
        UUID ownerId = createUser("owner_sprint2_guard", UserRole.RESTAURANT_OWNER);
        UUID anotherOwnerId = createUser("another_owner_sprint2_guard", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint2_guard", UserRole.CUSTOMER);
        var order = createPaidOrder(ownerId, customerId);

        assertThatThrownBy(() -> orderService.confirm(order.id(), anotherOwnerId, false))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deliveryAssignmentRequiresReadyForDeliveryAndDriverRole() {
        UUID ownerId = createUser("owner_sprint2_assign", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint2_assign", UserRole.CUSTOMER);
        UUID driverId = createAvailableDriver("driver_sprint2_assign");
        UUID nonDriverId = createUser("customer_not_driver_sprint2", UserRole.CUSTOMER);
        var paidOrder = createPaidOrder(ownerId, customerId);

        assertThatThrownBy(() -> deliveryService.assign(paidOrder.id(), new AssignDeliveryRequest(driverId)))
                .isInstanceOf(BusinessException.class);

        orderService.confirm(paidOrder.id(), ownerId, false);
        orderService.markPreparing(paidOrder.id(), ownerId, false);
        orderService.markReadyForDelivery(paidOrder.id(), ownerId, false);

        assertThatThrownBy(() -> deliveryService.assign(paidOrder.id(), new AssignDeliveryRequest(nonDriverId)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void driverCanOnlyOperateAssignedDelivery() {
        UUID ownerId = createUser("owner_sprint2_driver_guard", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint2_driver_guard", UserRole.CUSTOMER);
        UUID driverId = createAvailableDriver("driver_sprint2_driver_guard");
        UUID anotherDriverId = createUser("another_driver_sprint2_driver_guard", UserRole.DRIVER);
        var order = createPaidOrder(ownerId, customerId);
        orderService.confirm(order.id(), ownerId, false);
        orderService.markPreparing(order.id(), ownerId, false);
        orderService.markReadyForDelivery(order.id(), ownerId, false);
        var delivery = deliveryService.assign(order.id(), new AssignDeliveryRequest(driverId));

        assertThatThrownBy(() -> deliveryService.start(delivery.id(), anotherDriverId))
                .isInstanceOf(BusinessException.class);
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }

    private UUID createAvailableDriver(String username) {
        return createUser(username, UserRole.DRIVER);
    }

    private com.nova.food.domain.order.dto.response.OrderResponse createPaidOrder(UUID ownerId, UUID customerId) {
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Sprint 2 Kitchen " + ownerId,
                null,
                "Sprint 2 Street",
                null
        ), ownerId);
        var menuItem = menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Sprint 2 Dish", null, new BigDecimal("90000.00"), true),
                ownerId,
                false
        );
        var order = orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 1)
        )), customerId);
        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        return orderService.get(order.id(), customerId, false);
    }
}
