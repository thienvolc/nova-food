package com.techora.api.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techora.domain.category.dto.request.CategoryRequest;
import com.techora.domain.category.service.CategoryService;
import com.techora.domain.product.constant.ProductStatus;
import com.techora.domain.product.dto.request.ProductRequest;
import com.techora.domain.product.dto.response.ProductResponse;
import com.techora.domain.product.service.ProductService;
import com.techora.domain.user.entity.UserEntity;
import com.techora.domain.user.entity.UserRole;
import com.techora.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ApiSmokeTest {
    private static final String PASSWORD = "password";
    private static final String DESCRIPTION = "Smoke test item";
    private static final BigDecimal PRICE = BigDecimal.valueOf(45.00);
    private static final int STOCK_QUANTITY = 5;
    private static final int CART_QUANTITY = 2;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Test
    void goldenPathSupportsAuthCatalogCartCheckoutAndPayment() throws Exception {
        String customerToken = registerAndGetToken("smoke-customer");
        UserEntity admin = userService.createUser("smoke-admin", PASSWORD, UserRole.ADMIN);
        String adminToken = loginAndGetToken(admin.getUsername());

        UUID categoryId = createCategoryAsAdmin(adminToken);
        UUID productId = createProductAsAdmin(adminToken, categoryId);

        assertPublicCatalogContainsProduct();
        addProductToCart(customerToken, productId);

        UUID orderId = checkout(customerToken);
        UUID paymentId = createPayment(customerToken, orderId);
        confirmPayment(customerToken, paymentId);

        assertThat(productService.getAdminProduct(productId).stockQuantity())
                .isEqualTo(STOCK_QUANTITY - CART_QUANTITY);
    }

    private String registerAndGetToken(String username) throws Exception {
        MvcResult result = mockMvc().perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return accessToken(result);
    }

    private String loginAndGetToken(String username) throws Exception {
        MvcResult result = mockMvc().perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return accessToken(result);
    }

    private UUID createCategoryAsAdmin(String adminToken) throws Exception {
        MvcResult result = mockMvc().perform(post("/api/v1/admin/categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Smoke Category",
                                  "description": "%s",
                                  "active": true
                                }
                                """.formatted(DESCRIPTION)))
                .andExpect(status().isOk())
                .andReturn();
        return dataId(result);
    }

    private UUID createProductAsAdmin(String adminToken, UUID categoryId) throws Exception {
        MvcResult result = mockMvc().perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Smoke Product",
                                  "sku": "SMOKE-PRODUCT-001",
                                  "description": "%s",
                                  "price": %s,
                                  "stockQuantity": %d,
                                  "categoryId": "%s",
                                  "status": "ACTIVE"
                                }
                                """.formatted(DESCRIPTION, PRICE, STOCK_QUANTITY, categoryId)))
                .andExpect(status().isOk())
                .andReturn();
        return dataId(result);
    }

    private void assertPublicCatalogContainsProduct() throws Exception {
        ProductResponse product = productService.getPublicProducts(null, "Smoke", null, null).items().getFirst();

        mockMvc().perform(get("/api/v1/products/" + product.slug()))
                .andExpect(status().isOk());
    }

    private void addProductToCart(String customerToken, UUID productId) throws Exception {
        mockMvc().perform(post("/api/v1/cart/items")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "quantity": %d
                                }
                                """.formatted(productId, CART_QUANTITY)))
                .andExpect(status().isOk());
    }

    private UUID checkout(String customerToken) throws Exception {
        MvcResult result = mockMvc().perform(post("/api/v1/orders/checkout")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andReturn();
        return dataId(result);
    }

    private UUID createPayment(String customerToken, UUID orderId) throws Exception {
        MvcResult result = mockMvc().perform(post("/api/v1/payments")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andReturn();
        return dataId(result);
    }

    private void confirmPayment(String customerToken, UUID paymentId) throws Exception {
        mockMvc().perform(post("/api/v1/payments/" + paymentId + "/confirm")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk());
    }

    private UUID dataId(MvcResult result) throws Exception {
        return UUID.fromString(data(result).get("id").asText());
    }

    private String accessToken(MvcResult result) throws Exception {
        return data(result).get("accessToken").asText();
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }
}
