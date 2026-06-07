package com.nova.food.domain.core;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.menu.dto.request.CreateMenuItemRequest;
import com.nova.food.domain.menu.service.MenuItemService;
import com.nova.food.domain.order.dto.request.CreateOrderRequest;
import com.nova.food.domain.order.repository.OrderRepository;
import com.nova.food.domain.order.service.OrderService;
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
class Sprint11OrderIdempotencyTests {

    @Autowired
    private UserService userService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void repeatedOrderSubmissionWithSameIdempotencyKeyReturnsSameOrder() {
        UUID ownerId = createUser("owner_sprint11_same_key", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint11_same_key", UserRole.CUSTOMER);
        UUID menuItemId = createMenuItem(ownerId);
        CreateOrderRequest request = orderRequest(menuItemId, 1);

        var first = orderService.create(request, customerId, "order-key-1");
        var second = orderService.create(request, customerId, "order-key-1");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).hasSize(1);
    }

    @Test
    void reusingSameIdempotencyKeyWithDifferentPayloadIsRejected() {
        UUID ownerId = createUser("owner_sprint11_conflict", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint11_conflict", UserRole.CUSTOMER);
        UUID menuItemId = createMenuItem(ownerId);

        orderService.create(orderRequest(menuItemId, 1), customerId, "order-key-conflict");

        assertThatThrownBy(() -> orderService.create(orderRequest(menuItemId, 2), customerId, "order-key-conflict"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void repeatedOrderSubmissionWithoutIdempotencyKeyCreatesDistinctOrders() {
        UUID ownerId = createUser("owner_sprint11_no_key", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint11_no_key", UserRole.CUSTOMER);
        UUID menuItemId = createMenuItem(ownerId);
        CreateOrderRequest request = orderRequest(menuItemId, 1);

        var first = orderService.create(request, customerId, null);
        var second = orderService.create(request, customerId, null);

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).hasSize(2);
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }

    private UUID createMenuItem(UUID ownerId) {
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Sprint 11 Kitchen " + UUID.randomUUID(),
                null,
                "Sprint 11 Street",
                null
        ), ownerId);
        return menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Sprint 11 Dish", null, new BigDecimal("51000.00"), true, true, 10, 1),
                ownerId,
                false
        ).id();
    }

    private CreateOrderRequest orderRequest(UUID menuItemId, int quantity) {
        return new CreateOrderRequest(List.of(new CreateOrderRequest.Item(menuItemId, quantity)));
    }
}
