package com.nova.food.domain.payment.service;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.common.dto.PageResponse;
import com.nova.food.domain.common.service.IdempotencyKeyService;
import com.nova.food.domain.common.service.IdempotencyLockManager;
import com.nova.food.domain.common.service.PageValidator;
import com.nova.food.domain.order.entity.OrderEntity;
import com.nova.food.domain.order.policy.OrderPolicyService;
import com.nova.food.domain.order.service.OrderService;
import com.nova.food.domain.events.outbox.OutboxService;
import com.nova.food.domain.payment.constant.PaymentMethod;
import com.nova.food.domain.payment.constant.PaymentStatus;
import com.nova.food.domain.payment.dto.event.PaymentCompletedEvent;
import com.nova.food.domain.payment.dto.event.PaymentFailedEvent;
import com.nova.food.domain.payment.dto.request.MockPaymentRequest;
import com.nova.food.domain.payment.dto.response.PaymentResponse;
import com.nova.food.domain.payment.entity.PaymentTransactionEntity;
import com.nova.food.domain.payment.mapper.PaymentMapper;
import com.nova.food.domain.payment.repository.PaymentTransactionRepository;
import com.nova.food.infrastructure.adapter.payment.PaymentGateway;
import com.nova.food.infrastructure.adapter.payment.PaymentGatewayResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentMapper paymentMapper;
    private final OrderService orderService;
    private final OrderPolicyService orderPolicyService;
    private final IdempotencyLockManager idempotencyLockManager;
    private final IdempotencyKeyService idempotencyKeyService;
    private final PaymentIdempotencyService paymentIdempotencyService;
    private final OutboxService outboxService;
    private final PageValidator pageValidator;

    @Transactional
    public PaymentResponse payMock(UUID orderId, MockPaymentRequest request, UUID customerId) {
        return payMock(orderId, request, customerId, null);
    }

    @Transactional
    public PaymentResponse payMock(UUID orderId, MockPaymentRequest request, UUID customerId, String idempotencyKey) {
        String normalizedKey = idempotencyKeyService.normalize(idempotencyKey);
        if (normalizedKey == null) {
            return payMockInternal(orderId, request, customerId, null);
        }
        String lockKey = "payment:" + orderId + ":" + normalizedKey;
        return idempotencyLockManager.executeWithLock(
                lockKey,
                () -> payMockInternal(orderId, request, customerId, normalizedKey)
        );
    }

    private PaymentResponse payMockInternal(UUID orderId, MockPaymentRequest request, UUID customerId, String normalizedKey) {
        OrderEntity order = orderService.getRequiredOrder(orderId);
        orderService.validateOwnsOrder(order, customerId);
        var existing = paymentIdempotencyService.findExisting(orderId, normalizedKey);
        if (existing.isPresent()) {
            return paymentMapper.toResponse(existing.get());
        }
        orderPolicyService.requireStatus(order, com.nova.food.domain.order.constant.OrderStatus.PENDING_PAYMENT);

        PaymentGatewayResult result = paymentGateway.pay(orderId, order.getTotal(), request.approved());
        Instant now = Instant.now();
        PaymentTransactionEntity savedPayment;
        try {
            savedPayment = paymentTransactionRepository.save(PaymentTransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .amount(order.getTotal())
                    .status(result.success() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED)
                    .method(PaymentMethod.MOCK)
                    .provider("mock")
                    .providerTransactionId(result.providerTransactionId())
                    .idempotencyKey(normalizedKey)
                    .failureReason(result.failureReason())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            return paymentIdempotencyService.recoverFromUniqueViolation(orderId, normalizedKey, ex)
                    .map(paymentMapper::toResponse)
                    .orElseThrow(() -> ex);
        }
        if (result.success()) {
            orderService.markPaid(order, customerId);
            outboxService.append(new PaymentCompletedEvent(
                    savedPayment.getId(),
                    orderId,
                    savedPayment.getAmount(),
                    Instant.now()
            ));
        } else {
            outboxService.append(new PaymentFailedEvent(
                    savedPayment.getId(),
                    orderId,
                    savedPayment.getAmount(),
                    result.failureReason(),
                    Instant.now()
            ));
        }
        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listByOrder(UUID orderId, UUID userId, boolean admin) {
        OrderEntity order = orderService.getRequiredOrder(orderId);
        if (!admin) {
            orderService.validateOwnsOrder(order, userId);
        }
        return paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> listByOrder(UUID orderId, UUID userId, boolean admin, int page, int size) {
        pageValidator.validate(page, size);
        OrderEntity order = orderService.getRequiredOrder(orderId);
        if (!admin) {
            orderService.validateOwnsOrder(order, userId);
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(paymentTransactionRepository.findByOrderId(orderId, pageable).map(paymentMapper::toResponse));
    }

}
