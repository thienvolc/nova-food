package com.techora.domain.idempotency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techora.domain.idempotency.constant.IdempotencyOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class IdempotencyRequestHashService {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String OPERATION = "operation";

    private final ObjectMapper objectMapper;

    public String hash(IdempotencyOperation operation, Map<String, Object> attributes) {
        return sha256(toJson(normalize(operation, attributes)));
    }

    private Map<String, Object> normalize(IdempotencyOperation operation, Map<String, Object> attributes) {
        Map<String, Object> normalized = new TreeMap<>(attributes);
        normalized.put(OPERATION, operation.name());
        return normalized;
    }

    private String toJson(Map<String, Object> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize idempotency request", ex);
        }
    }

    private String sha256(String value) {
        byte[] digest = digest(value);
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte item : digest) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }

    private byte[] digest(String value) {
        try {
            return MessageDigest.getInstance(HASH_ALGORITHM).digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
