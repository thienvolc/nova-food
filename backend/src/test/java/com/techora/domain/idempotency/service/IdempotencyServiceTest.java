package com.techora.domain.idempotency.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.cart.dto.request.AddCartItemRequest;
import com.techora.domain.cart.service.CartService;
import com.techora.domain.category.dto.request.CategoryRequest;
import com.techora.domain.category.service.CategoryService;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.order.dto.response.OrderResponse;
import com.techora.domain.order.repository.OrderRepository;
import com.techora.domain.order.service.OrderService;
import com.techora.domain.payment.constant.PaymentStatus;
import com.techora.domain.payment.dto.request.CreatePaymentRequest;
import com.techora.domain.payment.dto.response.PaymentResponse;
import com.techora.domain.payment.service.PaymentService;
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
class IdempotencyServiceTest {
    private static final String DESCRIPTION = "Idempotency test item";
    private static final BigDecimal PRICE = BigDecimal.valueOf(40.00);
    private static final int STOCK_QUANTITY = 5;
    private static final int ORDER_QUANTITY = 2;
    private static final String CHECKOUT_KEY = "checkout-key";
    private static final String PAYMENT_KEY = "payment-key";

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CartService cartService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Test
    void checkoutReplaysSameKeyWithoutCreatingDuplicateOrder() {
        UserEntity user = createUser("idempotency-owner-a");
        ProductResponse product = createProduct("Idempotency Shoe A", "IDEMPOTENCY-A");
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), ORDER_QUANTITY));

        OrderResponse firstOrder = orderService.checkout(user.getId(), CHECKOUT_KEY);
        OrderResponse replayedOrder = orderService.checkout(user.getId(), CHECKOUT_KEY);

        assertThat(replayedOrder.id()).isEqualTo(firstOrder.id());
        assertThat(orderRepository.countByUserId(user.getId())).isEqualTo(1);
    }

    @Test
    void sameKeyCanBeUsedByDifferentUsers() {
        UserEntity firstUser = createUser("idempotency-owner-b");
        UserEntity secondUser = createUser("idempotency-owner-c");
        addCartItem(firstUser, "Idempotency Shoe B", "IDEMPOTENCY-B");
        addCartItem(secondUser, "Idempotency Shoe C", "IDEMPOTENCY-C");

        orderService.checkout(firstUser.getId(), CHECKOUT_KEY);

        assertThat(orderService.checkout(secondUser.getId(), CHECKOUT_KEY).id()).isNotNull();
    }

    @Test
    void reusedCheckoutKeyForChangedRequestIsRejected() {
        UserEntity user = createUser("idempotency-owner-d");
        addCartItem(user, "Idempotency Shoe D", "IDEMPOTENCY-D");
        orderService.checkout(user.getId(), CHECKOUT_KEY);

        assertThatThrownBy(() -> paymentService.failPayment(user.getId(), UUID.randomUUID(), CHECKOUT_KEY))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.IDEMPOTENCY_KEY_CONFLICT.getDefaultMessage());
    }

    @Test
    void paymentConfirmReplaysSameKeyWithoutInvalidTransition() {
        UserEntity user = createUser("idempotency-owner-e");
        OrderResponse order = createOrder(user, "Idempotency Shoe E", "IDEMPOTENCY-E");
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));

        PaymentResponse paidPayment = paymentService.confirmPayment(user.getId(), payment.id(), PAYMENT_KEY);
        PaymentResponse replayedPayment = paymentService.confirmPayment(user.getId(), payment.id(), PAYMENT_KEY);

        assertThat(paidPayment.id()).isEqualTo(replayedPayment.id());
        assertThat(replayedPayment.status()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void paymentKeyConflictRejectsDifferentOperation() {
        UserEntity user = createUser("idempotency-owner-f");
        OrderResponse order = createOrder(user, "Idempotency Shoe F", "IDEMPOTENCY-F");
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));

        paymentService.confirmPayment(user.getId(), payment.id(), PAYMENT_KEY);

        assertThatThrownBy(() -> paymentService.failPayment(user.getId(), payment.id(), PAYMENT_KEY))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.IDEMPOTENCY_KEY_CONFLICT.getDefaultMessage());
    }

    @Test
    void paymentKeyConflictRejectsDifferentPaymentRequest() {
        UserEntity user = createUser("idempotency-owner-g");
        PaymentResponse firstPayment = createPayment(user, "Idempotency Shoe G", "IDEMPOTENCY-G");
        PaymentResponse secondPayment = createPayment(user, "Idempotency Shoe H", "IDEMPOTENCY-H");

        paymentService.confirmPayment(user.getId(), firstPayment.id(), PAYMENT_KEY);

        assertThatThrownBy(() -> paymentService.confirmPayment(user.getId(), secondPayment.id(), PAYMENT_KEY))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.IDEMPOTENCY_KEY_CONFLICT.getDefaultMessage());
    }

    private OrderResponse createOrder(UserEntity user, String productName, String sku) {
        ProductResponse product = createProduct(productName, sku);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), ORDER_QUANTITY));
        return orderService.checkout(user.getId());
    }

    private PaymentResponse createPayment(UserEntity user, String productName, String sku) {
        OrderResponse order = createOrder(user, productName, sku);
        return paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));
    }

    private void addCartItem(UserEntity user, String productName, String sku) {
        ProductResponse product = createProduct(productName, sku);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), ORDER_QUANTITY));
    }

    private UserEntity createUser(String username) {
        return userService.createUser(username, "password");
    }

    private ProductResponse createProduct(String name, String sku) {
        UUID categoryId = categoryService.create(new CategoryRequest(name + " Category", DESCRIPTION, true)).id();
        return productService.create(new ProductRequest(
                name,
                sku,
                DESCRIPTION,
                PRICE,
                STOCK_QUANTITY,
                categoryId,
                ProductStatus.ACTIVE
        ));
    }
}
