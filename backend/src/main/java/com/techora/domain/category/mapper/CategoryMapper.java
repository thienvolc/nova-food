package com.techora.domain.category.mapper;

import com.techora.domain.category.dto.response.CategoryResponse;
import com.techora.domain.category.entity.CategoryEntity;
import org.springframework.stereotype.Service;

@Service
public class CategoryMapper {
    public CategoryResponse toResponse(CategoryEntity entity) {
        return new CategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
