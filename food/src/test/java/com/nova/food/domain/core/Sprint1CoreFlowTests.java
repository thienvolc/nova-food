package com.nova.food.domain.core;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.auth.service.AuthService;
import com.nova.food.domain.menu.dto.request.CreateMenuItemRequest;
import com.nova.food.domain.menu.service.MenuItemService;
import com.nova.food.domain.order.constant.OrderStatus;
import com.nova.food.domain.order.dto.request.CreateOrderRequest;
import com.nova.food.domain.order.service.OrderService;
import com.nova.food.domain.payment.constant.PaymentStatus;
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
class Sprint1CoreFlowTests {

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
    void ownerCreatesRestaurantAndMenuThenCustomerCreatesOrderAndPays() {
        UUID ownerId = createUser("owner_core_flow", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_core_flow", UserRole.CUSTOMER);
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Nova Pizza",
                "Fast pizza",
                "1 Nova Street",
                "0900000000"
        ), ownerId);
        var menuItem = menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Margherita", "Classic pizza", new BigDecimal("120000.00"), true),
                ownerId,
                false
        );

        var order = orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 2)
        )), customerId);
        var payment = paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        var paidOrder = orderService.get(order.id(), customerId, false);

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.total()).isEqualByComparingTo("240000.00");
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().getFirst().menuItemName()).isEqualTo("Margherita");
        assertThat(payment.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(paidOrder.status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void orderRejectsUnavailableMenuItem() {
        UUID ownerId = createUser("owner_unavailable", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_unavailable", UserRole.CUSTOMER);
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Closed Kitchen",
                null,
                "2 Nova Street",
                null
        ), ownerId);
        var menuItem = menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Hidden Dish", null, new BigDecimal("50000.00"), false),
                ownerId,
                false
        );

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 1)
        )), customerId)).isInstanceOf(BusinessException.class);
    }

    @Test
    void paymentFailureDoesNotMarkOrderPaid() {
        UUID ownerId = createUser("owner_payment_fail", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_payment_fail", UserRole.CUSTOMER);
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Nova Noodles",
                null,
                "3 Nova Street",
                null
        ), ownerId);
        var menuItem = menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Beef Noodles", null, new BigDecimal("80000.00"), true),
                ownerId,
                false
        );
        var order = orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 1)
        )), customerId);

        var payment = paymentService.payMock(order.id(), new MockPaymentRequest(false), customerId);
        var pendingOrder = orderService.get(order.id(), customerId, false);

        assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(pendingOrder.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }
}
