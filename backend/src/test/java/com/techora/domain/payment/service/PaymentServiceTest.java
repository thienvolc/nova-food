package com.techora.domain.payment.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.cart.dto.request.AddCartItemRequest;
import com.techora.domain.cart.service.CartService;
import com.techora.domain.category.dto.request.CategoryRequest;
import com.techora.domain.category.service.CategoryService;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.order.constant.OrderStatus;
import com.techora.domain.order.dto.response.OrderResponse;
import com.techora.domain.order.service.OrderService;
import com.techora.domain.payment.constant.PaymentStatus;
import com.techora.domain.payment.dto.request.CreatePaymentRequest;
import com.techora.domain.payment.dto.response.PaymentResponse;
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
class PaymentServiceTest {
    private static final String DESCRIPTION = "Payment test item";
    private static final BigDecimal PRICE = BigDecimal.valueOf(30.00);
    private static final int STOCK_QUANTITY = 5;
    private static final int ORDER_QUANTITY = 2;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Test
    void createPaymentCreatesPendingPaymentForOwnerOrder() {
        UserEntity user = createUser("payment-owner-a");
        OrderResponse order = createOrder(user, "Payment Shoe A", "PAYMENT-SHOE-A");

        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));
        OrderResponse paymentPendingOrder = orderService.getUserOrder(user.getId(), order.id());

        assertThat(payment.orderId()).isEqualTo(order.id());
        assertThat(payment.userId()).isEqualTo(user.getId());
        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.amount()).isEqualByComparingTo(order.total());
        assertThat(payment.providerReference()).startsWith("mock_");
        assertThat(paymentPendingOrder.status()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void confirmPaymentMarksPaymentPaidAndOrderConfirmed() {
        UserEntity user = createUser("payment-owner-b");
        OrderResponse order = createOrder(user, "Payment Shoe B", "PAYMENT-SHOE-B");
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));

        PaymentResponse paidPayment = paymentService.confirmPayment(user.getId(), payment.id());
        OrderResponse paidOrder = orderService.getUserOrder(user.getId(), order.id());

        assertThat(paidPayment.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(paidOrder.status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void createPaymentRejectsDuplicateOrderPayment() {
        UserEntity user = createUser("payment-owner-c");
        OrderResponse order = createOrder(user, "Payment Shoe C", "PAYMENT-SHOE-C");
        CreatePaymentRequest request = new CreatePaymentRequest(order.id());

        paymentService.createPayment(user.getId(), request);

        assertThatThrownBy(() -> paymentService.createPayment(user.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.PAYMENT_ALREADY_EXISTS.getDefaultMessage());
    }

    @Test
    void failPaymentCancelsOrderAndRejectsLaterConfirm() {
        UserEntity user = createUser("payment-owner-d");
        OrderResponse order = createOrder(user, "Payment Shoe D", "PAYMENT-SHOE-D");
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));

        paymentService.failPayment(user.getId(), payment.id());
        OrderResponse failedOrder = orderService.getUserOrder(user.getId(), order.id());

        assertThat(failedOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> paymentService.confirmPayment(user.getId(), payment.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.INVALID_PAYMENT_STATUS_TRANSITION.getDefaultMessage());
    }

    private OrderResponse createOrder(UserEntity user, String productName, String sku) {
        ProductResponse product = createProduct(productName, sku);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), ORDER_QUANTITY));
        return orderService.checkout(user.getId());
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
