package com.nova.food.domain.core;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.delivery.dto.request.AssignDeliveryRequest;
import com.nova.food.domain.delivery.service.DeliveryService;
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
class Sprint9WorkflowHardeningTests {

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
    void invalidOrderTransitionsAreRejectedWithoutMutatingOrder() {
        UUID ownerId = createUser("owner_sprint9_status", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint9_status", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId, 3);

        assertThatThrownBy(() -> orderService.confirm(order.id(), ownerId, false))
                .isInstanceOf(BusinessException.class);

        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        orderService.confirm(order.id(), ownerId, false);

        assertThatThrownBy(() -> orderService.markReadyForDelivery(order.id(), ownerId, false))
                .isInstanceOf(BusinessException.class);

        var current = orderService.get(order.id(), customerId, false);
        assertThat(current.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void completedPaymentBlocksFurtherPaymentAttempts() {
        UUID ownerId = createUser("owner_sprint9_payment", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint9_payment", UserRole.CUSTOMER);
        var order = createOrder(ownerId, customerId, 2);

        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);

        assertThatThrownBy(() -> paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void sameOrderCannotBeAssignedTwice() {
        UUID ownerId = createUser("owner_sprint9_delivery", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint9_delivery", UserRole.CUSTOMER);
        UUID firstDriverId = createUser("driver1_sprint9_delivery", UserRole.DRIVER);
        UUID secondDriverId = createUser("driver2_sprint9_delivery", UserRole.DRIVER);
        var order = readyOrder(ownerId, customerId, 2);

        deliveryService.assign(order.id(), new AssignDeliveryRequest(firstDriverId));

        assertThatThrownBy(() -> deliveryService.assign(order.id(), new AssignDeliveryRequest(secondDriverId)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelledPaidOrderRestoresStockOnceAndTooLateCancellationDoesNotChangeIt() {
        UUID ownerId = createUser("owner_sprint9_stock", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint9_stock", UserRole.CUSTOMER);
        var menuItem = createTrackedMenuItem(ownerId, 5);
        var order = orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 2)
        )), customerId);

        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        assertThat(menuItemService.getRequiredMenuItem(menuItem.id()).getStockQuantity()).isEqualTo(3);

        orderService.cancelByCustomer(order.id(), new CancelOrderRequest("Changed mind"), customerId);
        assertThat(menuItemService.getRequiredMenuItem(menuItem.id()).getStockQuantity()).isEqualTo(5);

        var secondOrder = readyOrder(ownerId, customerId, 2);
        assertThatThrownBy(() -> orderService.cancelByRestaurant(
                secondOrder.id(),
                new CancelOrderRequest("Too late"),
                ownerId,
                false
        )).isInstanceOf(BusinessException.class);

        assertThat(menuItemService.getRequiredMenuItem(extractMenuItemId(secondOrder.id())).getStockQuantity()).isEqualTo(1);
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }

    private com.nova.food.domain.order.dto.response.OrderResponse createOrder(UUID ownerId, UUID customerId, int stock) {
        var menuItem = createTrackedMenuItem(ownerId, stock);
        return orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 1)
        )), customerId);
    }

    private com.nova.food.domain.menu.dto.response.MenuItemResponse createTrackedMenuItem(UUID ownerId, int stock) {
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Sprint 9 Kitchen " + UUID.randomUUID(),
                null,
                "Sprint 9 Street",
                null
        ), ownerId);
        return menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Sprint 9 Dish", null, new BigDecimal("90000.00"), true, true, stock, 1),
                ownerId,
                false
        );
    }

    private com.nova.food.domain.order.dto.response.OrderResponse readyOrder(UUID ownerId, UUID customerId, int stock) {
        var order = createOrder(ownerId, customerId, stock);
        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        orderService.confirm(order.id(), ownerId, false);
        orderService.markPreparing(order.id(), ownerId, false);
        return orderService.markReadyForDelivery(order.id(), ownerId, false);
    }

    private UUID extractMenuItemId(UUID orderId) {
        return orderService.get(orderId, orderService.getRequiredOrder(orderId).getCustomerId(), false)
                .items()
                .getFirst()
                .menuItemId();
    }
}
