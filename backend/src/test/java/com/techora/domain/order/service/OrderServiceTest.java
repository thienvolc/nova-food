package com.techora.domain.order.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.cart.dto.request.AddCartItemRequest;
import com.techora.domain.cart.service.CartService;
import com.techora.domain.category.dto.request.CategoryRequest;
import com.techora.domain.category.service.CategoryService;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.order.constant.OrderStatus;
import com.techora.domain.order.dto.response.OrderResponse;
import com.techora.domain.order.repository.OrderRepository;
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
class OrderServiceTest {
    private static final String DESCRIPTION = "Order test item";
    private static final BigDecimal PRICE = BigDecimal.valueOf(25.00);
    private static final int STOCK_QUANTITY = 5;
    private static final int CART_QUANTITY = 2;
    private static final int LOW_STOCK_QUANTITY = 1;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Test
    void checkoutCreatesOrderSnapshotsItemsAndKeepsStockReserved() {
        UserEntity user = createUser("order-owner-a");
        ProductResponse product = createProduct("Order Shoe A", "ORDER-SHOE-A", STOCK_QUANTITY);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), CART_QUANTITY));

        OrderResponse order = orderService.checkout(user.getId());
        ProductResponse updatedProduct = productService.getAdminProduct(product.id());

        assertThat(order.status()).isEqualTo(OrderStatus.STOCK_RESERVED);
        assertThat(order.items()).hasSize(1);
        assertThat(order.total()).isEqualByComparingTo(PRICE.multiply(BigDecimal.valueOf(CART_QUANTITY)));
        assertThat(updatedProduct.stockQuantity()).isEqualTo(STOCK_QUANTITY);
        assertThat(cartService.getCart(user.getId()).items()).isEmpty();
    }

    @Test
    void checkoutRejectsEmptyCart() {
        UserEntity user = createUser("order-owner-b");

        assertThatThrownBy(() -> orderService.checkout(user.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.CART_EMPTY.getDefaultMessage());
    }

    @Test
    void checkoutRejectsInsufficientStockAtCheckoutTime() {
        UserEntity user = createUser("order-owner-c");
        ProductResponse product = createProduct("Order Shoe C", "ORDER-SHOE-C", LOW_STOCK_QUANTITY);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), LOW_STOCK_QUANTITY));
        productService.update(product.id(), productRequest(
                "Order Shoe C",
                "ORDER-SHOE-C",
                ProductStatus.ACTIVE,
                product.category().id(),
                0
        ));

        assertThatThrownBy(() -> orderService.checkout(user.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.INSUFFICIENT_STOCK.getDefaultMessage());
    }

    @Test
    void userOrderHistoryOnlyReturnsOwnerOrders() {
        UserEntity owner = createUser("order-owner-d");
        UserEntity otherUser = createUser("order-owner-e");
        ProductResponse product = createProduct("Order Shoe D", "ORDER-SHOE-D", STOCK_QUANTITY);
        cartService.addItem(owner.getId(), new AddCartItemRequest(product.id(), CART_QUANTITY));
        OrderResponse order = orderService.checkout(owner.getId());

        assertThat(orderRepository.countByUserId(owner.getId())).isEqualTo(1);
        assertThat(orderService.getUserOrders(otherUser.getId(), null, null).items())
                .extracting(OrderResponse::id)
                .doesNotContain(order.id());
    }

    @Test
    void updateStatusRejectsInvalidTransition() {
        UserEntity user = createUser("order-owner-f");
        ProductResponse product = createProduct("Order Shoe F", "ORDER-SHOE-F", STOCK_QUANTITY);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), CART_QUANTITY));
        OrderResponse order = orderService.checkout(user.getId());

        assertThatThrownBy(() -> orderService.updateStatus(order.id(), OrderStatus.DELIVERED))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.INVALID_ORDER_STATUS_TRANSITION.getDefaultMessage());
    }

    @Test
    void adminCanMovePaidOrderThroughFulfillmentFlow() {
        UserEntity user = createUser("order-owner-g");
        ProductResponse product = createProduct("Order Shoe G", "ORDER-SHOE-G", STOCK_QUANTITY);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), CART_QUANTITY));
        OrderResponse order = orderService.checkout(user.getId());
        orderService.requestPayment(order.id());
        orderService.markPaid(order.id(), user.getId(), user.getUsername());

        OrderResponse fulfillingOrder = orderService.updateStatus(order.id(), OrderStatus.FULFILLING);
        OrderResponse shippedOrder = orderService.updateStatus(order.id(), OrderStatus.SHIPPED);
        OrderResponse deliveredOrder = orderService.updateStatus(order.id(), OrderStatus.DELIVERED);

        assertThat(fulfillingOrder.status()).isEqualTo(OrderStatus.FULFILLING);
        assertThat(shippedOrder.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(deliveredOrder.status()).isEqualTo(OrderStatus.DELIVERED);
    }

    private UserEntity createUser(String username) {
        return userService.createUser(username, "password");
    }

    private ProductResponse createProduct(String name, String sku, int stockQuantity) {
        UUID categoryId = categoryService.create(new CategoryRequest(name + " Category", DESCRIPTION, true)).id();
        return productService.create(productRequest(name, sku, ProductStatus.ACTIVE, categoryId, stockQuantity));
    }

    private ProductRequest productRequest(
            String name,
            String sku,
            ProductStatus status,
            UUID categoryId,
            int stockQuantity
    ) {
        return new ProductRequest(
                name,
                sku,
                DESCRIPTION,
                PRICE,
                stockQuantity,
                categoryId,
                status
        );
    }
}
