package com.nova.food.domain.menu.repository;

import com.nova.food.domain.menu.entity.StockAdjustmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustmentEntity, UUID> {
}
