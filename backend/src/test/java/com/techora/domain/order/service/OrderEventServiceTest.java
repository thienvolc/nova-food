package com.techora.domain.order.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.cart.dto.request.AddCartItemRequest;
import com.techora.domain.cart.service.CartService;
import com.techora.domain.category.dto.request.CategoryRequest;
import com.techora.domain.category.service.CategoryService;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.order.constant.OrderEventActorType;
import com.techora.domain.order.constant.OrderEventType;
import com.techora.domain.order.constant.OrderStatus;
import com.techora.domain.order.dto.response.AdminOrderEventResponse;
import com.techora.domain.order.dto.response.OrderEventResponse;
import com.techora.domain.order.dto.response.OrderResponse;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OrderEventServiceTest {
    private static final String DESCRIPTION = "Order event test item";
    private static final BigDecimal PRICE = BigDecimal.valueOf(35.00);
    private static final int STOCK_QUANTITY = 5;
    private static final int ORDER_QUANTITY = 2;

    @Autowired
    private OrderService orderService;

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
    void checkoutRecordsUserSafeOrderPlacedEvent() {
        UserEntity user = createUser("order-event-owner-a");
        OrderResponse order = createOrder(user, "Order Event Shoe A", "ORDER-EVENT-A");

        List<OrderEventResponse> events = orderService.getUserOrderEvents(user.getId(), order.id());

        assertThat(events).hasSize(2);
        assertThat(events.getFirst().eventType()).isEqualTo(OrderEventType.ORDER_PLACED);
        assertThat(events.getFirst().newStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(events.getLast().newStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
    }

    @Test
    void paymentFailureRecordsOrderedAuditTimeline() {
        UserEntity user = createUser("order-event-owner-b");
        OrderResponse order = createOrder(user, "Order Event Shoe B", "ORDER-EVENT-B");
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));

        paymentService.failPayment(user.getId(), payment.id());
        List<AdminOrderEventResponse> events = orderService.getAdminOrderEvents(order.id());

        assertThat(events).extracting(AdminOrderEventResponse::eventType).containsExactly(
                OrderEventType.ORDER_PLACED,
                OrderEventType.ORDER_STATUS_CHANGED,
                OrderEventType.ORDER_STATUS_CHANGED,
                OrderEventType.PAYMENT_FAILED,
                OrderEventType.ORDER_CANCELLED
        );
        assertThat(events.getLast().actorType()).isEqualTo(OrderEventActorType.USER);
        assertThat(events.getLast().newStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void paymentConfirmationRecordsOrderedAuditTimeline() {
        UserEntity user = createUser("order-event-owner-e");
        OrderResponse order = createOrder(user, "Order Event Shoe E", "ORDER-EVENT-E");
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));

        paymentService.confirmPayment(user.getId(), payment.id());
        List<AdminOrderEventResponse> events = orderService.getAdminOrderEvents(order.id());

        assertThat(events).extracting(AdminOrderEventResponse::eventType).containsExactly(
                OrderEventType.ORDER_PLACED,
                OrderEventType.ORDER_STATUS_CHANGED,
                OrderEventType.ORDER_STATUS_CHANGED,
                OrderEventType.PAYMENT_CONFIRMED
        );
        assertThat(events.getLast().newStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void adminStatusUpdateRecordsActorAndStatusChange() {
        UserEntity user = createUser("order-event-owner-c");
        UserEntity admin = createUser("order-event-admin-c");
        OrderResponse order = createOrder(user, "Order Event Shoe C", "ORDER-EVENT-C");

        orderService.updateStatus(order.id(), OrderStatus.PAYMENT_PENDING, admin.getId(), admin.getUsername());
        List<AdminOrderEventResponse> events = orderService.getAdminOrderEvents(order.id());

        assertThat(events).extracting(AdminOrderEventResponse::eventType).containsExactly(
                OrderEventType.ORDER_PLACED,
                OrderEventType.ORDER_STATUS_CHANGED,
                OrderEventType.ORDER_STATUS_CHANGED
        );
        assertThat(events.getLast().oldStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
        assertThat(events.getLast().newStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(events.getLast().actorType()).isEqualTo(OrderEventActorType.ADMIN);
        assertThat(events.getLast().actorId()).isEqualTo(admin.getId());
    }

    @Test
    void userOrderEventsRejectsNonOwner() {
        UserEntity owner = createUser("order-event-owner-d");
        UserEntity otherUser = createUser("order-event-other-d");
        OrderResponse order = createOrder(owner, "Order Event Shoe D", "ORDER-EVENT-D");

        assertThatThrownBy(() -> orderService.getUserOrderEvents(otherUser.getId(), order.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.ORDER_NOT_FOUND.getDefaultMessage());
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
