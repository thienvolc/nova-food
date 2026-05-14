package com.techora.domain.payment.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentReferenceService {
    private static final String REFERENCE_PREFIX = "mock_";

    public String createReference() {
        return REFERENCE_PREFIX + UUID.randomUUID();
    }
}
