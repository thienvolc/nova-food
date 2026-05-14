package com.techora.domain.outbox.service;

import com.techora.domain.order.constant.OrderStatus;
import com.techora.domain.order.entity.OrderEntity;
import com.techora.domain.outbox.constant.OutboxAggregateType;
import com.techora.domain.outbox.constant.OutboxEventType;
import com.techora.domain.outbox.entity.OutboxEventEntity;
import com.techora.domain.outbox.repository.OutboxEventRepository;
import com.techora.domain.payment.entity.PaymentEntity;
import com.techora.domain.product.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventService {
    private static final String ORDER_ID = "orderId";
    private static final String USER_ID = "userId";
    private static final String PAYMENT_ID = "paymentId";
    private static final String PAYMENT_STATUS = "paymentStatus";
    private static final String OLD_STATUS = "oldStatus";
    private static final String NEW_STATUS = "newStatus";
    private static final String ORDER_STATUS = "orderStatus";
    private static final String PRODUCT_ID = "productId";
    private static final String SKU = "sku";
    private static final String QUANTITY = "quantity";
    private static final String STOCK_QUANTITY = "stockQuantity";
    private static final String TOTAL = "total";
    private static final String AMOUNT = "amount";

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventFactory outboxEventFactory;

    public void recordOrderPlaced(OrderEntity order) {
        save(OutboxAggregateType.ORDER, order.getId(), OutboxEventType.ORDER_PLACED, Map.of(
                ORDER_ID, order.getId(),
                USER_ID, order.getUser().getId(),
                ORDER_STATUS, order.getStatus().name(),
                TOTAL, order.getTotal()
        ));
    }

    public void recordStockReduced(ProductEntity product, int quantity) {
        save(OutboxAggregateType.PRODUCT, product.getId(), OutboxEventType.STOCK_REDUCED, Map.of(
                PRODUCT_ID, product.getId(),
                SKU, product.getSku(),
                QUANTITY, quantity,
                STOCK_QUANTITY, product.getStockQuantity()
        ));
    }

    public void recordOrderStatusChanged(OrderEntity order, OrderStatus oldStatus, OrderStatus newStatus) {
        save(OutboxAggregateType.ORDER, order.getId(), OutboxEventType.ORDER_STATUS_CHANGED, Map.of(
                ORDER_ID, order.getId(),
                USER_ID, order.getUser().getId(),
                OLD_STATUS, oldStatus.name(),
                NEW_STATUS, newStatus.name()
        ));
    }

    public void recordPaymentConfirmed(PaymentEntity payment) {
        savePaymentEvent(payment, OutboxEventType.PAYMENT_CONFIRMED);
    }

    public void recordPaymentFailed(PaymentEntity payment) {
        savePaymentEvent(payment, OutboxEventType.PAYMENT_FAILED);
    }

    public void recordOrderCancelled(OrderEntity order) {
        save(OutboxAggregateType.ORDER, order.getId(), OutboxEventType.ORDER_CANCELLED, Map.of(
                ORDER_ID, order.getId(),
                USER_ID, order.getUser().getId(),
                ORDER_STATUS, order.getStatus().name()
        ));
    }

    private void savePaymentEvent(PaymentEntity payment, OutboxEventType eventType) {
        save(OutboxAggregateType.PAYMENT, payment.getId(), eventType, Map.of(
                PAYMENT_ID, payment.getId(),
                ORDER_ID, payment.getOrder().getId(),
                USER_ID, payment.getUser().getId(),
                PAYMENT_STATUS, payment.getStatus().name(),
                AMOUNT, payment.getAmount()
        ));
    }

    private void save(
            OutboxAggregateType aggregateType,
            UUID aggregateId,
            OutboxEventType eventType,
            Map<String, Object> attributes
    ) {
        outboxEventRepository.save(outboxEventFactory.create(aggregateType, aggregateId, eventType, attributes));
    }
}
