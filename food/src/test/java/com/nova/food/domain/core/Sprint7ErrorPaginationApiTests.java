package com.nova.food.domain.core;

import com.nova.food.domain.delivery.dto.request.AssignDeliveryRequest;
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
import com.nova.food.infrastructure.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class Sprint7ErrorPaginationApiTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtService jwtService;

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
    private com.nova.food.domain.delivery.service.DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void validationErrorsUseStandardApiBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": "",
                                  "role": "CUSTOMER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("validation.failed"))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors.length()").value(4))
                .andExpect(jsonPath("$.validationErrors[0].field").exists());
    }

    @Test
    void parsingAndSecurityErrorsUseStandardApiBody() throws Exception {
        UUID customerId = createUser("customer_sprint7_http", UserRole.CUSTOMER);
        String customerToken = bearerToken(customerId, "customer_sprint7_http", UserRole.CUSTOMER);
        String ownerToken = bearerToken(createUser("owner_sprint7_http", UserRole.RESTAURANT_OWNER),
                "owner_sprint7_http", UserRole.RESTAURANT_OWNER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR_INVALID_REQUEST"));

        mockMvc.perform(get("/api/v1/orders/not-a-uuid")
                        .header(HttpHeaders.AUTHORIZATION, customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR_INVALID_REQUEST"));

        mockMvc.perform(get("/api/v1/orders/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR_INVALID_TOKEN"));

        mockMvc.perform(get("/api/v1/orders/my")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR_ACCESS_DENIED"));
    }

    @Test
    void paginatedEndpointsReturnPageResponseShape() throws Exception {
        UUID ownerId = createUser("owner_sprint7_api_page", UserRole.RESTAURANT_OWNER);
        UUID customerId = createUser("customer_sprint7_api_page", UserRole.CUSTOMER);
        UUID driverId = createDriver("driver_sprint7_api_page");
        CompletedOrder order = completedOrder(ownerId, customerId, driverId);

        mockMvc.perform(get("/api/v1/orders/my")
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(customerId, "customer_sprint7_api_page", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1));

        mockMvc.perform(get("/api/v1/orders/restaurants/{restaurantId}", order.restaurantId())
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(ownerId, "owner_sprint7_api_page", UserRole.RESTAURANT_OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1));

        mockMvc.perform(get("/api/v1/orders/{orderId}/payments", order.orderId())
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(customerId, "customer_sprint7_api_page", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1));

        mockMvc.perform(get("/api/v1/deliveries/my")
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(driverId, "driver_sprint7_api_page", UserRole.DRIVER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void invalidPaginationUsesBusinessErrorContract() throws Exception {
        UUID customerId = createUser("customer_sprint7_bad_page", UserRole.CUSTOMER);

        mockMvc.perform(get("/api/v1/orders/my")
                        .queryParam("page", "-1")
                        .queryParam("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(customerId, "customer_sprint7_bad_page", UserRole.CUSTOMER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR_INVALID_PAGE_REQUEST"))
                .andExpect(jsonPath("$.message").value("page.request.invalid"));
    }

    private UUID createUser(String username, UserRole role) {
        return userService.createUser(username, "Password123!", role).getId();
    }

    private UUID createDriver(String username) {
        return createUser(username, UserRole.DRIVER);
    }

    private String bearerToken(UUID userId, String username, UserRole role) {
        return "Bearer " + jwtService.generateToken(userId, username, role.name());
    }

    private CompletedOrder completedOrder(UUID ownerId, UUID customerId, UUID driverId) {
        var restaurant = restaurantService.create(new CreateRestaurantRequest(
                "Sprint 7 API Kitchen " + UUID.randomUUID(),
                null,
                "Sprint 7 API Street",
                null
        ), ownerId);
        var menuItem = menuItemService.create(
                restaurant.id(),
                new CreateMenuItemRequest("Sprint 7 API Dish", null, new BigDecimal("80000.00"), true, true, 10, 1),
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
