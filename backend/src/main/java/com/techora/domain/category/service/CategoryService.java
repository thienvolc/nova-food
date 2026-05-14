package com.techora.domain.category.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.category.dto.request.CategoryRequest;
import com.techora.domain.category.dto.response.CategoryResponse;
import com.techora.domain.category.entity.CategoryEntity;
import com.techora.domain.category.mapper.CategoryMapper;
import com.techora.domain.category.repository.CategoryRepository;
import com.techora.domain.common.constant.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private static final Sort NAME_ASCENDING = Sort.by(Sort.Direction.ASC, "name");

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SlugService slugService;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String slug = slugService.createSlug(request.name());
        validateUniqueCategory(request.name(), slug);

        CategoryEntity category = buildCategory(request, slug);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll(NAME_ASCENDING).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryEntity getRequiredEntity(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CATEGORY_NOT_FOUND));
    }

    @Transactional
    public CategoryResponse update(UUID categoryId, CategoryRequest request) {
        CategoryEntity category = getRequiredEntity(categoryId);
        String slug = slugService.createSlug(request.name());
        validateUniqueCategory(categoryId, request.name(), slug);
        updateCategory(category, request, slug);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID categoryId) {
        CategoryEntity category = getRequiredEntity(categoryId);
        categoryRepository.delete(category);
    }

    private void validateUniqueCategory(String name, String slug) {
        if (categoryRepository.existsByNameIgnoreCase(name) || categoryRepository.existsBySlug(slug)) {
            throw new BusinessException(ResponseCode.CATEGORY_ALREADY_EXISTS);
        }
    }

    private void validateUniqueCategory(UUID categoryId, String name, String slug) {
        boolean nameExists = categoryRepository.existsByNameIgnoreCaseAndIdNot(name, categoryId);
        boolean slugExists = categoryRepository.existsBySlugAndIdNot(slug, categoryId);
        if (nameExists || slugExists) {
            throw new BusinessException(ResponseCode.CATEGORY_ALREADY_EXISTS);
        }
    }

    private CategoryEntity buildCategory(CategoryRequest request, String slug) {
        Instant now = Instant.now();
        return CategoryEntity.builder()
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .active(resolveActive(request.active()))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void updateCategory(CategoryEntity category, CategoryRequest request, String slug) {
        category.setName(request.name());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setActive(resolveActive(request.active()));
        category.setUpdatedAt(Instant.now());
    }

    private boolean resolveActive(Boolean active) {
        return active == null || active;
    }
}
