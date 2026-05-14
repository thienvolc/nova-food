package com.techora.domain.inventory.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.cart.dto.request.AddCartItemRequest;
import com.techora.domain.cart.service.CartService;
import com.techora.domain.category.dto.request.CategoryRequest;
import com.techora.domain.category.service.CategoryService;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.inventory.constant.InventoryReservationStatus;
import com.techora.domain.inventory.entity.InventoryReservationEntity;
import com.techora.domain.inventory.repository.InventoryReservationRepository;
import com.techora.domain.order.dto.response.OrderResponse;
import com.techora.domain.order.service.OrderService;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class InventoryReservationServiceTest {
    private static final String DESCRIPTION = "Reservation test item";
    private static final BigDecimal PRICE = BigDecimal.valueOf(45.00);
    private static final int STOCK_QUANTITY = 5;
    private static final int ORDER_QUANTITY = 2;
    private static final int LOW_STOCK_QUANTITY = 1;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private InventoryReservationService inventoryReservationService;

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
    void checkoutReservesInventoryWithoutReducingStock() {
        UserEntity user = createUser("reservation-owner-a");
        ProductResponse product = createProduct("Reservation Shoe A", "RESERVATION-A", STOCK_QUANTITY);
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), ORDER_QUANTITY));

        OrderResponse order = orderService.checkout(user.getId());
        List<InventoryReservationEntity> reservations = findReservations(order.id());

        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().getStatus()).isEqualTo(InventoryReservationStatus.RESERVED);
        assertThat(reservations.getFirst().getQuantity()).isEqualTo(ORDER_QUANTITY);
        assertThat(productService.getAdminProduct(product.id()).stockQuantity()).isEqualTo(STOCK_QUANTITY);
    }

    @Test
    void checkoutConsidersExistingReservedQuantity() {
        UserEntity firstUser = createUser("reservation-owner-b");
        UserEntity secondUser = createUser("reservation-owner-c");
        ProductResponse product = createProduct("Reservation Shoe B", "RESERVATION-B", LOW_STOCK_QUANTITY);
        cartService.addItem(firstUser.getId(), new AddCartItemRequest(product.id(), LOW_STOCK_QUANTITY));
        cartService.addItem(secondUser.getId(), new AddCartItemRequest(product.id(), LOW_STOCK_QUANTITY));

        orderService.checkout(firstUser.getId());

        assertThatThrownBy(() -> orderService.checkout(secondUser.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResponseCode.INSUFFICIENT_STOCK.getDefaultMessage());
    }

    @Test
    void paymentConfirmationCommitsReservationAndReducesStock() {
        UserEntity user = createUser("reservation-owner-d");
        ProductResponse product = createProduct("Reservation Shoe D", "RESERVATION-D", STOCK_QUANTITY);
        OrderResponse order = createOrder(user, product);
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));

        paymentService.confirmPayment(user.getId(), payment.id());
        List<InventoryReservationEntity> reservations = findReservations(order.id());

        assertThat(reservations.getFirst().getStatus()).isEqualTo(InventoryReservationStatus.CONFIRMED);
        assertThat(productService.getAdminProduct(product.id()).stockQuantity())
                .isEqualTo(STOCK_QUANTITY - ORDER_QUANTITY);
    }

    @Test
    void paymentFailureReleasesReservationWithoutReducingStock() {
        UserEntity user = createUser("reservation-owner-e");
        ProductResponse product = createProduct("Reservation Shoe E", "RESERVATION-E", STOCK_QUANTITY);
        OrderResponse order = createOrder(user, product);
        PaymentResponse payment = paymentService.createPayment(user.getId(), new CreatePaymentRequest(order.id()));

        paymentService.failPayment(user.getId(), payment.id());
        List<InventoryReservationEntity> reservations = findReservations(order.id());

        assertThat(reservations.getFirst().getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);
        assertThat(productService.getAdminProduct(product.id()).stockQuantity()).isEqualTo(STOCK_QUANTITY);
    }

    @Test
    void expiryServiceMarksAbandonedReservationsAsExpired() {
        UserEntity user = createUser("reservation-owner-f");
        ProductResponse product = createProduct("Reservation Shoe F", "RESERVATION-F", STOCK_QUANTITY);
        OrderResponse order = createOrder(user, product);
        InventoryReservationEntity reservation = findReservations(order.id()).getFirst();
        reservation.setExpiresAt(Instant.now().minusSeconds(1));
        inventoryReservationRepository.save(reservation);

        int expiredCount = inventoryReservationService.expireAbandonedReservations();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(findReservations(order.id()).getFirst().getStatus()).isEqualTo(InventoryReservationStatus.EXPIRED);
    }

    private OrderResponse createOrder(UserEntity user, ProductResponse product) {
        cartService.addItem(user.getId(), new AddCartItemRequest(product.id(), ORDER_QUANTITY));
        return orderService.checkout(user.getId());
    }

    private List<InventoryReservationEntity> findReservations(UUID orderId) {
        return inventoryReservationRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    private UserEntity createUser(String username) {
        return userService.createUser(username, "password");
    }

    private ProductResponse createProduct(String name, String sku, int stockQuantity) {
        UUID categoryId = categoryService.create(new CategoryRequest(name + " Category", DESCRIPTION, true)).id();
        return productService.create(new ProductRequest(
                name,
                sku,
                DESCRIPTION,
                PRICE,
                stockQuantity,
                categoryId,
                ProductStatus.ACTIVE
        ));
    }
}
