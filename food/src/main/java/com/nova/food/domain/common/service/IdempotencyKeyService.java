package com.nova.food.domain.common.service;

import org.springframework.stereotype.Service;

@Service
public class IdempotencyKeyService {

    public String normalize(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String trimmed = idempotencyKey.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
