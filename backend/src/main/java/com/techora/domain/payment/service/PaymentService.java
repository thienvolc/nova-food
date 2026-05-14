package com.techora.domain.payment.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.idempotency.constant.IdempotencyOperation;
import com.techora.domain.idempotency.service.IdempotencyService;
import com.techora.domain.order.dto.response.OrderResponse;
import com.techora.domain.order.entity.OrderEntity;
import com.techora.domain.order.repository.OrderRepository;
import com.techora.domain.order.service.OrderService;
import com.techora.domain.outbox.service.OutboxEventService;
import com.techora.domain.payment.constant.PaymentStatus;
import com.techora.domain.payment.dto.request.CreatePaymentRequest;
import com.techora.domain.payment.dto.response.PaymentResponse;
import com.techora.domain.payment.entity.PaymentEntity;
import com.techora.domain.payment.mapper.PaymentMapper;
import com.techora.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final String PAYMENT_ID = "paymentId";
    private static final String USER_ID = "userId";

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentReferenceService paymentReferenceService;
    private final PaymentStatusPolicy paymentStatusPolicy;
    private final OutboxEventService outboxEventService;
    private final IdempotencyService idempotencyService;

    @Transactional
    public PaymentResponse createPayment(UUID userId, CreatePaymentRequest request) {
        OrderEntity order = getRequiredUserOrder(userId, request.orderId());
        validatePaymentDoesNotExist(order.getId());
        OrderResponse paymentPendingOrder = orderService.requestPayment(order.getId());
        order = orderRepository.findWithItemsById(paymentPendingOrder.id()).orElse(order);

        PaymentEntity payment = buildPayment(order);
        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID userId, UUID paymentId) {
        PaymentEntity payment = paymentRepository.findWithOrderByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse confirmPayment(UUID userId, UUID paymentId) {
        return confirmPaymentWithoutIdempotency(userId, paymentId);
    }

    @Transactional
    public PaymentResponse confirmPayment(UUID userId, UUID paymentId, String idempotencyKey) {
        return executeIdempotentPayment(
                userId,
                paymentId,
                idempotencyKey,
                IdempotencyOperation.PAYMENT_CONFIRM,
                () -> confirmPaymentWithoutIdempotency(userId, paymentId)
        );
    }

    private PaymentResponse confirmPaymentWithoutIdempotency(UUID userId, UUID paymentId) {
        PaymentEntity payment = getRequiredUserPayment(userId, paymentId);
        transitionPayment(payment, PaymentStatus.PAID);
        String actorName = payment.getUser().getUsername();
        OrderResponse paidOrder = orderService.markPaid(payment.getOrder().getId(), userId, actorName);
        payment.setOrder(orderRepository.findWithItemsById(paidOrder.id()).orElse(payment.getOrder()));
        PaymentEntity savedPayment = paymentRepository.save(payment);
        outboxEventService.recordPaymentConfirmed(savedPayment);
        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional
    public PaymentResponse failPayment(UUID userId, UUID paymentId) {
        return failPaymentWithoutIdempotency(userId, paymentId);
    }

    @Transactional
    public PaymentResponse failPayment(UUID userId, UUID paymentId, String idempotencyKey) {
        return executeIdempotentPayment(
                userId,
                paymentId,
                idempotencyKey,
                IdempotencyOperation.PAYMENT_FAIL,
                () -> failPaymentWithoutIdempotency(userId, paymentId)
        );
    }

    private PaymentResponse failPaymentWithoutIdempotency(UUID userId, UUID paymentId) {
        PaymentEntity payment = getRequiredUserPayment(userId, paymentId);
        transitionPayment(payment, PaymentStatus.FAILED);
        String actorName = payment.getUser().getUsername();
        OrderResponse cancelledOrder = orderService.markPaymentFailed(payment.getOrder().getId(), userId, actorName);
        payment.setOrder(orderRepository.findWithItemsById(cancelledOrder.id()).orElse(payment.getOrder()));
        PaymentEntity savedPayment = paymentRepository.save(payment);
        outboxEventService.recordPaymentFailed(savedPayment);
        return paymentMapper.toResponse(savedPayment);
    }

    private PaymentResponse executeIdempotentPayment(
            UUID userId,
            UUID paymentId,
            String idempotencyKey,
            IdempotencyOperation operation,
            Supplier<PaymentResponse> action
    ) {
        return idempotencyService.execute(
                userId,
                idempotencyKey,
                operation,
                Map.of(USER_ID, userId, PAYMENT_ID, paymentId),
                PaymentResponse.class,
                action
        );
    }

    private OrderEntity getRequiredUserOrder(UUID userId, UUID orderId) {
        return orderRepository.findWithItemsByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }

    private PaymentEntity getRequiredUserPayment(UUID userId, UUID paymentId) {
        return paymentRepository.findWithOrderByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }

    private void validatePaymentDoesNotExist(UUID orderId) {
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ResponseCode.PAYMENT_ALREADY_EXISTS);
        }
    }

    private PaymentEntity buildPayment(OrderEntity order) {
        Instant now = Instant.now();
        return PaymentEntity.builder()
                .order(order)
                .user(order.getUser())
                .amount(order.getTotal())
                .status(PaymentStatus.PENDING)
                .providerReference(paymentReferenceService.createReference())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void transitionPayment(PaymentEntity payment, PaymentStatus nextStatus) {
        validateStatusTransition(payment.getStatus(), nextStatus);
        payment.setStatus(nextStatus);
        payment.setUpdatedAt(Instant.now());
    }

    private void validateStatusTransition(PaymentStatus currentStatus, PaymentStatus nextStatus) {
        if (!paymentStatusPolicy.canTransition(currentStatus, nextStatus)) {
            throw new BusinessException(ResponseCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
    }
}
