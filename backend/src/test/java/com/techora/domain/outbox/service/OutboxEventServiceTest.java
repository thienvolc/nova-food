package com.techora.domain.outbox.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.cart.dto.request.AddCartItemRequest;
import com.techora.domain.cart.service.CartService;
import com.techora.domain.category.dto.request.CategoryRequest;
import com.techora.domain.category.service.CategoryService;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.order.dto.response.OrderResponse;
import com.techora.domain.order.service.OrderService;
import com.techora.domain.outbox.constant.OutboxAggregateType;
import com.techora.domain.outbox.constant.OutboxEventStatus;
import com.techora.domain.outbox.constant.OutboxEventType;
import com.techora.domain.outbox.entity.OutboxEventEntity;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OutboxEventServiceTest {
    private static final String DESCRIPTION = "Outbox test item";
    private static final BigDecimal PRICE = BigDecimal.valueOf(35.00);
    private static final int STOCK_QUANTITY = 5;
    private static final int ORDER_QUANTITY = 2;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

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
    void checkoutPersistsOrderOutboxEventAtomically() {
        UserEntity user = createUser("outbox-owner-a");
        ProductResponse product = createProduct("Outbox Shoe A", "OUTBOX-SHOE-A", STOCK_QUANTITY);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), ORDER_QUANTITY));

        OrderResponse order = orderService.checkout(user.getId());

        assertThat(outboxEventRepository.countByAggregateTypeAndAggregateIdAndEventType(
                OutboxAggregateType.ORDER,
                order.id(),
                OutboxEventType.ORDER_PLACED
        )).isEqualTo(1);
        assertThat(outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                OutboxAggregateType.PRODUCT,
                product.id()
        )).isEmpty();
    }

    @Test
    void failedCheckoutDoesNotPersistOutboxEvents() {
        UserEntity user = createUser("outbox-owner-b");
        ProductResponse product = createProduct("Outbox Shoe B", "OUTBOX-SHOE-B", STOCK_QUANTITY);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), ORDER_QUANTITY));
        productService.update(product.id(), productRequest(
                "Outbox Shoe B",
                "OUTBOX-SHOE-B",
                ProductStatus.ACTIVE,
                product.category().id(),
                0
        ));

        assertThatThrownBy(() -> orderService.checkout(user.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.INSUFFICIENT_STOCK.getDefaultMessage());
        assertThat(outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                OutboxAggregateType.PRODUCT,
                product.id()
        )).isEmpty();
    }

    @Test
    void paymentTransitionsPersistOutboxEvents() {
        UserEntity paidUser = createUser("outbox-owner-c");
        PaymentResponse paidPayment = createPayment(paidUser, "Outbox Shoe C", "OUTBOX-SHOE-C");
        UserEntity failedUser = createUser("outbox-owner-d");
        PaymentResponse failedPayment = createPayment(failedUser, "Outbox Shoe D", "OUTBOX-SHOE-D");

        paymentService.confirmPayment(paidUser.getId(), paidPayment.id());
        paymentService.failPayment(failedUser.getId(), failedPayment.id());

        assertThat(outboxEventRepository.countByAggregateTypeAndAggregateIdAndEventType(
                OutboxAggregateType.PAYMENT,
                paidPayment.id(),
                OutboxEventType.PAYMENT_CONFIRMED
        )).isEqualTo(1);
        assertThat(outboxEventRepository.countByAggregateTypeAndAggregateIdAndEventType(
                OutboxAggregateType.PAYMENT,
                failedPayment.id(),
                OutboxEventType.PAYMENT_FAILED
        )).isEqualTo(1);
    }

    @Test
    void publisherMarksPendingEventsAsPublishedOrFailed() {
        outboxPublisherService.publishPendingEvents(1000, event -> {});
        UserEntity user = createUser("outbox-owner-e");
        ProductResponse product = createProduct("Outbox Shoe E", "OUTBOX-SHOE-E", STOCK_QUANTITY);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), ORDER_QUANTITY));
        OrderResponse order = orderService.checkout(user.getId());
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));
        paymentService.confirmPayment(user.getId(), payment.id());

        int publishedCount = outboxPublisherService.publishPendingEvents(1, event -> {});
        OutboxEventEntity publishedEvent = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PUBLISHED,
                org.springframework.data.domain.PageRequest.of(0, 1)
        ).getFirst();

        assertThat(publishedCount).isEqualTo(1);
        assertThat(publishedEvent.getProcessedAt()).isNotNull();

        outboxPublisherService.publishPendingEvents(1, event -> {
            throw new IllegalStateException("publisher failure");
        });

        assertThat(checkoutOutboxStatuses(product.id(), order.id()))
                .contains(OutboxEventStatus.PUBLISHED, OutboxEventStatus.FAILED);
    }

    private PaymentResponse createPayment(UserEntity user, String productName, String sku) {
        ProductResponse product = createProduct(productName, sku, STOCK_QUANTITY);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), ORDER_QUANTITY));
        OrderResponse order = orderService.checkout(user.getId());
        return paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));
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

    private List<OutboxEventStatus> checkoutOutboxStatuses(UUID productId, UUID orderId) {
        return Stream.concat(
                outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.PRODUCT,
                        productId
                ).stream(),
                outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.ORDER,
                        orderId
                ).stream()
        ).map(OutboxEventEntity::getStatus).toList();
    }
}
