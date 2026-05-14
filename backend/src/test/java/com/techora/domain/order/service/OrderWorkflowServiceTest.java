package com.techora.domain.order.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.cart.dto.request.AddCartItemRequest;
import com.techora.domain.cart.service.CartService;
import com.techora.domain.category.dto.request.CategoryRequest;
import com.techora.domain.category.service.CategoryService;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.inventory.constant.InventoryReservationStatus;
import com.techora.domain.inventory.entity.InventoryReservationEntity;
import com.techora.domain.inventory.repository.InventoryReservationRepository;
import com.techora.domain.order.constant.OrderEventType;
import com.techora.domain.order.constant.OrderStatus;
import com.techora.domain.order.dto.response.AdminOrderEventResponse;
import com.techora.domain.order.dto.response.OrderResponse;
import com.techora.domain.outbox.constant.OutboxAggregateType;
import com.techora.domain.outbox.constant.OutboxEventType;
import com.techora.domain.outbox.repository.OutboxEventRepository;
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
class OrderWorkflowServiceTest {
    private static final String DESCRIPTION = "Workflow test item";
    private static final BigDecimal PRICE = BigDecimal.valueOf(40.00);
    private static final int STOCK_QUANTITY = 5;
    private static final int ORDER_QUANTITY = 2;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Test
    void paymentSuccessCompletesSagaWorkflow() {
        UserEntity user = createUser("workflow-owner-a");
        ProductResponse product = createProduct("Workflow Shoe A", "WORKFLOW-A");
        OrderResponse order = createOrder(user, product);
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));

        paymentService.confirmPayment(user.getId(), payment.id());
        OrderResponse paidOrder = orderService.getUserOrder(user.getId(), order.id());
        List<AdminOrderEventResponse> events = orderService.getAdminOrderEvents(order.id());

        assertThat(paidOrder.status()).isEqualTo(OrderStatus.PAID);
        assertThat(findReservation(order.id()).getStatus()).isEqualTo(InventoryReservationStatus.CONFIRMED);
        assertThat(productService.getAdminProduct(product.id()).stockQuantity())
                .isEqualTo(STOCK_QUANTITY - ORDER_QUANTITY);
        assertThat(events).extracting(AdminOrderEventResponse::eventType).containsExactly(
                OrderEventType.ORDER_PLACED,
                OrderEventType.ORDER_STATUS_CHANGED,
                OrderEventType.ORDER_STATUS_CHANGED,
                OrderEventType.PAYMENT_CONFIRMED
        );
    }

    @Test
    void paymentFailureRollsBackReservationAndCancelsOrder() {
        UserEntity user = createUser("workflow-owner-b");
        ProductResponse product = createProduct("Workflow Shoe B", "WORKFLOW-B");
        OrderResponse order = createOrder(user, product);
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));

        paymentService.failPayment(user.getId(), payment.id());
        OrderResponse cancelledOrder = orderService.getUserOrder(user.getId(), order.id());
        List<AdminOrderEventResponse> events = orderService.getAdminOrderEvents(order.id());

        assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(findReservation(order.id()).getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);
        assertThat(productService.getAdminProduct(product.id()).stockQuantity()).isEqualTo(STOCK_QUANTITY);
        assertThat(events).extracting(AdminOrderEventResponse::eventType).containsExactly(
                OrderEventType.ORDER_PLACED,
                OrderEventType.ORDER_STATUS_CHANGED,
                OrderEventType.ORDER_STATUS_CHANGED,
                OrderEventType.PAYMENT_FAILED,
                OrderEventType.ORDER_CANCELLED
        );
    }

    @Test
    void invalidWorkflowTransitionIsRejected() {
        UserEntity user = createUser("workflow-owner-c");
        ProductResponse product = createProduct("Workflow Shoe C", "WORKFLOW-C");
        OrderResponse order = createOrder(user, product);

        assertThatThrownBy(() -> orderService.updateStatus(order.id(), OrderStatus.DELIVERED))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.INVALID_ORDER_STATUS_TRANSITION.getDefaultMessage());
    }

    @Test
    void paymentFailureCompensationWritesRollbackOutboxEvent() {
        UserEntity user = createUser("workflow-owner-d");
        ProductResponse product = createProduct("Workflow Shoe D", "WORKFLOW-D");
        OrderResponse order = createOrder(user, product);
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));

        paymentService.failPayment(user.getId(), payment.id());

        assertThat(outboxEventRepository.countByAggregateTypeAndAggregateIdAndEventType(
                OutboxAggregateType.ORDER,
                order.id(),
                OutboxEventType.ORDER_CANCELLED
        )).isEqualTo(1);
    }

    private InventoryReservationEntity findReservation(UUID orderId) {
        return inventoryReservationRepository.findByOrderIdOrderByCreatedAtAsc(orderId).getFirst();
    }

    private OrderResponse createOrder(UserEntity user, ProductResponse product) {
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
