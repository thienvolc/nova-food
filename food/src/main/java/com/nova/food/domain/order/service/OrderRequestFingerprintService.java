package com.nova.food.domain.order.service;

import com.nova.food.domain.order.dto.request.CreateOrderRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;

@Service
public class OrderRequestFingerprintService {

    private static final String HASH_ALGORITHM = "SHA-256";

    public String fingerprint(CreateOrderRequest request) {
        String canonicalRequest = canonicalize(request);
        return sha256(canonicalRequest);
    }

    private String canonicalize(CreateOrderRequest request) {
        return request.items().stream()
                .map(item -> new CanonicalItem(item.menuItemId().toString(), item.quantity()))
                .sorted(Comparator.comparing(CanonicalItem::menuItemId).thenComparingInt(CanonicalItem::quantity))
                .map(item -> item.menuItemId() + ":" + item.quantity())
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Missing hash algorithm", exception);
        }
    }

    private record CanonicalItem(String menuItemId, int quantity) {
    }
}
