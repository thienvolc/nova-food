package com.nova.food.domain.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.food.app.aop.AppExceptionHandler;
import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.delivery.dto.request.AssignDeliveryRequest;
import com.nova.food.domain.delivery.service.DeliveryService;
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
import com.nova.food.infrastructure.security.RestAccessDeniedHandler;
import com.nova.food.infrastructure.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class Sprint7ErrorPaginationTests {

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
    void parsingBusinessAndSecurityErrorsUseStandardJson() throws Exception {
        AppExceptionHandler handler = new AppExceptionHandler();

        var invalidRequest = handler.handleInvalidRequest(
                new MissingServletRequestParameterException("fromDate", "Instant")
        );
        var businessError = handler.handleException(new BusinessException(ResponseCode.ORDER_INVALID_STATUS));

        assertThat(invalidRequest.getStatusCode().value()).isEqualTo(400);
        assertThat(invalidRequest.getBody().getCode()).isEqualTo(ResponseCode.INVALID_REQUEST.getCode());
        assertThat(businessError.getStatusCode().value()).isEqualTo(400);
        assertThat(businessError.getBody().getCode()).isEqualTo(ResponseCode.ORDER_INVALID_STATUS.getCode());

        ObjectMapper objectMapper = new ObjectMapper();
        MockHttpServletResponse unauthenticated = new MockHttpServletResponse();
        new RestAuthenticationEntryPoint(objectMapper).commence(
                new MockHttpServletRequest(),
                unauthenticated,
                new BadCredentialsException("missing token")
        );
        MockHttpServletResponse forbidden = new MockHttpServletResponse();
        new RestAccessDeniedHandler(objectMapper).handle(
                new MockHttpServletRequest(),
                forbidden,
                new AccessDeniedException("forbidden")
        );

        assertThat(unauthenticated.getStatus()).isEqualTo(401);
        assertThat(unauthenticated.getContentAsString()).contains(ResponseCode.INVALID_TOKEN.getCode());
        assertThat(forbidden.getStatus()).isEqualTo(403);
        assertThat(forbidden.getContentAsString()).contains(ResponseCode.ACCESS_DENIED.getCode());
    }

    @Test
    void orderPaymentAndDeliveryListsArePaginated() {
        UUID ownerId = createUser("owner_sprint7_page", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint7_page", UserRole.CUSTOMER);
        UUID driverId = createDriver("driver_sprint7_page");
        var order = completedOrder(ownerId, customerId, driverId);

        var orders = orderService.listMyOrders(customerId, 0, 1);
        var restaurantOrders = orderService.listRestaurantOrders(order.restaurantId(), ownerId, false, 0, 1);
        var payments = paymentService.listByOrder(order.orderId(), customerId, false, 0, 1);
        var deliveries = deliveryService.listAssignedToDriver(driverId, 0, 1);

        assertThat(orders.page()).isZero();
        assertThat(orders.size()).isEqualTo(1);
        assertThat(orders.totalElements()).isEqualTo(1);
        assertThat(restaurantOrders.totalElements()).isEqualTo(1);
        assertThat(payments.totalElements()).isEqualTo(1);
        assertThat(deliveries.totalElements()).isEqualTo(1);
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }

    private UUID createDriver(String username) {
        return createUser(username, UserRole.DRIVER);
    }

    private CompletedOrder completedOrder(UUID ownerId, UUID customerId, UUID driverId) {
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Sprint 7 Kitchen " + UUID.randomUUID(),
                null,
                "Sprint 7 Street",
                null
        ), ownerId);
        var menuItem = menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Sprint 7 Dish", null, new BigDecimal("80000.00"), true, true, 10, 1),
                ownerId,
                false
        );
        var order = orderService.create(new CreateOrderRequest(List.of(
                new CreateOrderRequest.Item(menuItem.id(), 1)
        )), customerId);
        paymentService.payMock(order.id(), new MockPaymentRequest(true), customerId);
        orderService.confirm(order.id(), ownerId, false);
        orderService.markPreparing(order.id(), ownerId, false);
        orderService.markReadyForDelivery(order.id(), ownerId, false);
        var delivery = deliveryService.assign(order.id(), new AssignDeliveryRequest(driverId));
        deliveryService.start(delivery.id(), driverId);
        deliveryService.complete(delivery.id(), driverId);
        return new CompletedOrder(order.id(), restaurant.id());
    }

    private record CompletedOrder(UUID orderId, UUID restaurantId) {
    }
}
