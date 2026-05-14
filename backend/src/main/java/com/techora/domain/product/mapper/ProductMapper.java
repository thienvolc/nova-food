package com.techora.domain.product.mapper;

import com.techora.domain.category.mapper.CategoryMapper;
import com.techora.domain.product.dto.response.ProductResponse;
import com.techora.domain.product.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductMapper {
    private final CategoryMapper categoryMapper;

    public ProductResponse toResponse(ProductEntity entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getName(),
                entity.getSku(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getStockQuantity(),
                entity.getStatus(),
                categoryMapper.toResponse(entity.getCategory()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
