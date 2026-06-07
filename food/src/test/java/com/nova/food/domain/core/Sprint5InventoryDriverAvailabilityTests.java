package com.nova.food.domain.core;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.delivery.dto.request.AssignDeliveryRequest;
import com.nova.food.domain.delivery.service.DeliveryService;
import com.nova.food.domain.menu.dto.request.CreateMenuItemRequest;
import com.nova.food.domain.menu.dto.request.CreateStockAdjustmentRequest;
import com.nova.food.domain.menu.dto.request.UpdateMenuItemStockRequest;
import com.nova.food.domain.menu.service.MenuItemService;
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
class Sprint5InventoryDriverAvailabilityTests {

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
    void paymentDecrementsTrackedStockOnceAndCancellationRestoresIt() {
        UUID ownerId = createUser("owner_sprint5_stock", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint5_stock", UserRole.CUSTOMER);
        var menuItem = createTrackedMenuItem(ownerId, 4);
        var order = orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 2)
        )), customerId);

        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        var afterPayment = menuItemService.getRequiredMenuItem(menuItem.id());
        assertThat(afterPayment.getStockQuantity()).isEqualTo(2);

        orderService.cancelByCustomer(order.id(), new CancelOrderRequest("Restore stock"), customerId);
        var afterCancellation = menuItemService.getRequiredMenuItem(menuItem.id());

        assertThat(afterCancellation.getStockQuantity()).isEqualTo(4);
    }

    @Test
    void orderCreationAndPaymentRejectInsufficientTrackedStock() {
        UUID ownerId = createUser("owner_sprint5_oos", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint5_oos", UserRole.CUSTOMER);
        var menuItem = createTrackedMenuItem(ownerId, 1);

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 2)
        )), customerId)).isInstanceOf(BusinessException.class);

        var firstOrder = orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 1)
        )), customerId);
        var secondOrder = orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 1)
        )), customerId);
        paymentService.payMock(firstOrder.id(), new MockPaymentRequest(true), customerId);

        assertThatThrownBy(() -> paymentService.payMock(secondOrder.id(), new MockPaymentRequest(true), customerId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void ownerCanUpdateAndAdjustStock() {
        UUID ownerId = createUser("owner_sprint5_adjust", UserRole.RESTAURANT_OWNER);
        var menuItem = createTrackedMenuItem(ownerId, 3);

        menuItemService.updateStock(menuItem.id(), new UpdateMenuItemStockRequest(true, 10, 2), ownerId, false);
        var adjusted = menuItemService.adjustStock(
                menuItem.id(),
                new CreateStockAdjustmentRequest(-4, "Waste"),
                ownerId,
                false
        );

        assertThat(adjusted.stockQuantity()).isEqualTo(6);
        assertThatThrownBy(() -> menuItemService.adjustStock(
                menuItem.id(),
                new CreateStockAdjustmentRequest(-7, "Invalid"),
                ownerId,
                false
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void driverAssignmentUsesPlainDriverAccountAndCompletionClosesTheFlow() {
        UUID ownerId = createUser("owner_sprint5_driver", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint5_driver", UserRole.CUSTOMER);
        UUID driverId = createDriver("driver_sprint5_driver");
        var order = readyOrder(ownerId, customerId);
        var delivery = deliveryService.assign(order.id(), new AssignDeliveryRequest(driverId));
        var delivering = deliveryService.start(delivery.id(), driverId);
        var completed = deliveryService.complete(delivery.id(), driverId);

        assertThat(delivery.driverId()).isEqualTo(driverId);
        assertThat(delivering.status()).isEqualTo(com.nova.food.domain.delivery.constant.DeliveryStatus.DELIVERING);
        assertThat(completed.status()).isEqualTo(com.nova.food.domain.delivery.constant.DeliveryStatus.COMPLETED);
    }

    @Test
    void busyDriverCannotReceiveAnotherActiveDelivery() {
        UUID ownerId = createUser("owner_sprint5_busy", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint5_busy", UserRole.CUSTOMER);
        UUID driverId = createDriver("driver_sprint5_busy");
        var firstOrder = readyOrder(ownerId, customerId);
        var secondOrder = readyOrder(ownerId, customerId);

        deliveryService.assign(firstOrder.id(), new AssignDeliveryRequest(driverId));

        assertThatThrownBy(() -> deliveryService.assign(secondOrder.id(), new AssignDeliveryRequest(driverId)))
                .isInstanceOf(BusinessException.class);
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }

    private UUID createDriver(String username) {
        return createUser(username, UserRole.DRIVER);
    }

    private com.nova.food.domain.menu.dto.response.MenuItemResponse createTrackedMenuItem(UUID ownerId, int stock) {
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Sprint 5 Kitchen " + UUID.randomUUID(),
                null,
                "Sprint 5 Street",
                null
        ), ownerId);
        return menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Sprint 5 Dish", null, new BigDecimal("80000.00"), true, true, stock, 1),
                ownerId,
                false
        );
    }

    private com.nova.food.domain.order.dto.response.OrderResponse readyOrder(UUID ownerId, UUID customerId) {
        var order = orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(createTrackedMenuItem(ownerId, 5).id(), 1)
        )), customerId);
        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        orderService.confirm(order.id(), ownerId, false);
        orderService.markPreparing(order.id(), ownerId, false);
        return orderService.markReadyForDelivery(order.id(), ownerId, false);
    }
}
