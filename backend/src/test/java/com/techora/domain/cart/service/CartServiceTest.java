package com.techora.domain.cart.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.cart.dto.request.AddCartItemRequest;
import com.techora.domain.cart.dto.request.UpdateCartItemRequest;
import com.techora.domain.cart.dto.response.CartItemResponse;
import com.techora.domain.cart.dto.response.CartResponse;
import com.techora.domain.category.dto.request.CategoryRequest;
import com.techora.domain.category.service.CategoryService;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.product.constant.ProductStatus;
import com.techora.domain.product.dto.request.ProductRequest;
import com.techora.domain.product.dto.response.ProductResponse;
import com.techora.domain.product.service.ProductService;
import com.techora.domain.user.entity.UserEntity;
import com.techora.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CartServiceTest {
    private static final String DESCRIPTION = "Cart test item";
    private static final BigDecimal PRICE = BigDecimal.valueOf(12.50);
    private static final int STOCK_QUANTITY = 5;
    private static final int FIRST_QUANTITY = 2;
    private static final int SECOND_QUANTITY = 3;
    private static final int OVER_STOCK_QUANTITY = 6;
    private static final int UPDATED_QUANTITY = 4;

    @Autowired
    private CartService cartService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Test
    void addItemMergesQuantityAndCalculatesTotal() {
        UserEntity user = createUser("cart-owner-a");
        ProductResponse product = createProduct("Cart Shoe A", "CART-SHOE-A", ProductStatus.ACTIVE);

        cartService.addItem(user.getId(), addRequest(product.id(), FIRST_QUANTITY));
        CartResponse cart = cartService.addItem(user.getId(), addRequest(product.id(), SECOND_QUANTITY));

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.total()).isEqualByComparingTo(PRICE.multiply(BigDecimal.valueOf(STOCK_QUANTITY)));
        assertThat(cart.items().getFirst().quantity()).isEqualTo(STOCK_QUANTITY);
    }

    @Test
    void updateItemRecalculatesSubtotal() {
        UserEntity user = createUser("cart-owner-b");
        ProductResponse product = createProduct("Cart Shoe B", "CART-SHOE-B", ProductStatus.ACTIVE);
        CartItemResponse item = cartService.addItem(user.getId(), addRequest(product.id(), FIRST_QUANTITY))
                .items()
                .getFirst();

        CartResponse cart = cartService.updateItem(user.getId(), item.id(), new UpdateCartItemRequest(UPDATED_QUANTITY));

        assertThat(cart.items().getFirst().quantity()).isEqualTo(UPDATED_QUANTITY);
        assertThat(cart.total()).isEqualByComparingTo(PRICE.multiply(BigDecimal.valueOf(UPDATED_QUANTITY)));
    }

    @Test
    void addItemRejectsInactiveProduct() {
        UserEntity user = createUser("cart-owner-c");
        ProductResponse product = createProduct("Cart Shoe C", "CART-SHOE-C", ProductStatus.INACTIVE);

        assertThatThrownBy(() -> cartService.addItem(user.getId(), addRequest(product.id(), FIRST_QUANTITY)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.PRODUCT_UNAVAILABLE.getDefaultMessage());
    }

    @Test
    void addItemRejectsQuantityAboveStock() {
        UserEntity user = createUser("cart-owner-d");
        ProductResponse product = createProduct("Cart Shoe D", "CART-SHOE-D", ProductStatus.ACTIVE);

        assertThatThrownBy(() -> cartService.addItem(user.getId(), addRequest(product.id(), OVER_STOCK_QUANTITY)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.INSUFFICIENT_STOCK.getDefaultMessage());
    }

    @Test
    void removeItemOnlyAffectsOwnerCart() {
        UserEntity owner = createUser("cart-owner-e");
        UserEntity otherUser = createUser("cart-owner-f");
        ProductResponse product = createProduct("Cart Shoe E", "CART-SHOE-E", ProductStatus.ACTIVE);
        CartItemResponse item = cartService.addItem(owner.getId(), addRequest(product.id(), FIRST_QUANTITY))
                .items()
                .getFirst();

        assertThatThrownBy(() -> cartService.removeItem(otherUser.getId(), item.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.CART_ITEM_NOT_FOUND.getDefaultMessage());
    }

    private UserEntity createUser(String username) {
        return userService.createUser(username, "password");
    }

    private ProductResponse createProduct(String name, String sku, ProductStatus status) {
        UUID categoryId = categoryService.create(new CategoryRequest(name + " Category", DESCRIPTION, true)).id();
        return productService.create(new ProductRequest(
                name,
                sku,
                DESCRIPTION,
                PRICE,
                STOCK_QUANTITY,
                categoryId,
                status
        ));
    }

    private AddCartItemRequest addRequest(UUID productId, int quantity) {
        return new AddCartItemRequest(productId, quantity);
    }
}
