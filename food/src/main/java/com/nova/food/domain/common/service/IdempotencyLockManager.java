package com.nova.food.domain.common.service;

import java.util.function.Supplier;

public interface IdempotencyLockManager {
    <T> T executeWithLock(String lockKey, Supplier<T> action);
}
