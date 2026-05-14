package com.techora.domain.idempotency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techora.app.aop.BusinessException;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.idempotency.constant.IdempotencyOperation;
import com.techora.domain.idempotency.constant.IdempotencyStatus;
import com.techora.domain.idempotency.entity.IdempotencyKeyEntity;
import com.techora.domain.idempotency.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private static final int EXPIRATION_DAYS = 1;

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final IdempotencyRequestHashService requestHashService;
    private final ObjectMapper objectMapper;

    public <T> T execute(
            UUID userId,
            String idempotencyKey,
            IdempotencyOperation operation,
            Map<String, Object> requestAttributes,
            Class<T> responseType,
            Supplier<T> action
    ) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return action.get();
        }

        String normalizedKey = idempotencyKey.trim();
        String requestHash = requestHashService.hash(operation, requestAttributes);
        return idempotencyKeyRepository.findByUserIdAndIdempotencyKey(userId, normalizedKey)
                .map(existing -> resolveExisting(existing, operation, requestHash, responseType, action))
                .orElseGet(() -> executeAndSave(userId, normalizedKey, operation, requestHash, action));
    }

    private <T> T resolveExisting(
            IdempotencyKeyEntity existing,
            IdempotencyOperation operation,
            String requestHash,
            Class<T> responseType,
            Supplier<T> action
    ) {
        if (isExpired(existing)) {
            return executeAndUpdate(existing, operation, requestHash, action);
        }
        validateReplay(existing, operation, requestHash);
        return fromJson(existing.getResponsePayload(), responseType);
    }

    private <T> T executeAndSave(
            UUID userId,
            String idempotencyKey,
            IdempotencyOperation operation,
            String requestHash,
            Supplier<T> action
    ) {
        T response = action.get();
        idempotencyKeyRepository.save(buildEntity(userId, idempotencyKey, operation, requestHash, response));
        return response;
    }

    private <T> T executeAndUpdate(
            IdempotencyKeyEntity existing,
            IdempotencyOperation operation,
            String requestHash,
            Supplier<T> action
    ) {
        T response = action.get();
        updateEntity(existing, operation, requestHash, response);
        return response;
    }

    private void validateReplay(
            IdempotencyKeyEntity existing,
            IdempotencyOperation operation,
            String requestHash
    ) {
        if (existing.getOperation() != operation || !existing.getRequestHash().equals(requestHash)) {
            throw new BusinessException(ResponseCode.IDEMPOTENCY_KEY_CONFLICT);
        }
    }

    private boolean isExpired(IdempotencyKeyEntity existing) {
        return existing.getExpiresAt().isBefore(Instant.now());
    }

    private <T> IdempotencyKeyEntity buildEntity(
            UUID userId,
            String idempotencyKey,
            IdempotencyOperation operation,
            String requestHash,
            T response
    ) {
        Instant now = Instant.now();
        return IdempotencyKeyEntity.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .operation(operation)
                .requestHash(requestHash)
                .status(IdempotencyStatus.COMPLETED)
                .responsePayload(toJson(response))
                .expiresAt(now.plus(EXPIRATION_DAYS, ChronoUnit.DAYS))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private <T> void updateEntity(
            IdempotencyKeyEntity existing,
            IdempotencyOperation operation,
            String requestHash,
            T response
    ) {
        Instant now = Instant.now();
        existing.setOperation(operation);
        existing.setRequestHash(requestHash);
        existing.setStatus(IdempotencyStatus.COMPLETED);
        existing.setResponsePayload(toJson(response));
        existing.setExpiresAt(now.plus(EXPIRATION_DAYS, ChronoUnit.DAYS));
        existing.setUpdatedAt(now);
        idempotencyKeyRepository.save(existing);
    }

    private <T> String toJson(T response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize idempotency response", ex);
        }
    }

    private <T> T fromJson(String responsePayload, Class<T> responseType) {
        try {
            return objectMapper.readValue(responsePayload, responseType);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize idempotency response", ex);
        }
    }
}
