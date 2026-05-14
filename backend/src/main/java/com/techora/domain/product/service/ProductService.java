package com.techora.domain.product.service;

import com.techora.app.aop.BusinessException;
import com.techora.app.dto.response.PageResponse;
import com.techora.domain.category.entity.CategoryEntity;
import com.techora.domain.category.service.CategoryService;
import com.techora.domain.category.service.SlugService;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.product.constant.ProductStatus;
import com.techora.domain.product.dto.request.ProductRequest;
import com.techora.domain.product.dto.response.ProductResponse;
import com.techora.domain.product.entity.ProductEntity;
import com.techora.domain.product.mapper.ProductMapper;
import com.techora.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final SlugService slugService;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        CategoryEntity category = categoryService.getRequiredEntity(request.categoryId());
        String slug = slugService.createSlug(request.name());
        validateUniqueProduct(request.sku(), slug);

        ProductEntity product = buildProduct(request, category, slug);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getPublicProducts(
            UUID categoryId,
            String keyword,
            Integer page,
            Integer size
    ) {
        Page<ProductEntity> products = findPublicProducts(categoryId, keyword, page, size);
        return toPageResponse(products);
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublicProduct(String slug) {
        ProductEntity product = productRepository.findWithCategoryBySlug(slug)
                .filter(this::isActive)
                .orElseThrow(() -> new BusinessException(ResponseCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getAdminProduct(UUID productId) {
        return productMapper.toResponse(getRequiredEntity(productId));
    }

    @Transactional(readOnly = true)
    public ProductEntity getRequiredActiveEntity(UUID productId) {
        ProductEntity product = getRequiredEntity(productId);
        if (!isActive(product)) {
            throw new BusinessException(ResponseCode.PRODUCT_UNAVAILABLE);
        }
        return product;
    }

    public ProductEntity getRequiredActiveEntityForUpdate(UUID productId) {
        ProductEntity product = getRequiredEntityForUpdate(productId);
        if (!isActive(product)) {
            throw new BusinessException(ResponseCode.PRODUCT_UNAVAILABLE);
        }
        return product;
    }

    @Transactional
    public ProductResponse update(UUID productId, ProductRequest request) {
        ProductEntity product = getRequiredEntity(productId);
        CategoryEntity category = categoryService.getRequiredEntity(request.categoryId());
        String slug = slugService.createSlug(request.name());
        validateUniqueProduct(productId, request.sku(), slug);
        updateProduct(product, request, category, slug);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(UUID productId) {
        ProductEntity product = getRequiredEntity(productId);
        productRepository.delete(product);
    }

    public void reduceStock(ProductEntity product, int quantity) {
        reduceStockAndReturn(product, quantity);
    }

    public ProductEntity reduceStockAndReturn(ProductEntity product, int quantity) {
        ProductEntity lockedProduct = getRequiredEntityForUpdate(product.getId());
        if (!isActive(lockedProduct)) {
            throw new BusinessException(ResponseCode.PRODUCT_UNAVAILABLE);
        }
        validateStock(lockedProduct, quantity);
        lockedProduct.setStockQuantity(lockedProduct.getStockQuantity() - quantity);
        lockedProduct.setUpdatedAt(Instant.now());
        return lockedProduct;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getLowStockProducts(Integer threshold, Integer page, Integer size) {
        Page<ProductEntity> products = productRepository.findByStockQuantityLessThanEqual(
                resolveStockThreshold(threshold),
                PageRequest.of(resolvePage(page), resolveSize(size), DEFAULT_SORT)
        );
        return toPageResponse(products);
    }

    private Page<ProductEntity> findPublicProducts(UUID categoryId, String keyword, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(resolvePage(page), resolveSize(size), DEFAULT_SORT);
        Optional<String> normalizedKeyword = normalizeKeyword(keyword);

        if (categoryId != null && normalizedKeyword.isPresent()) {
            return productRepository.findByStatusAndCategoryIdAndCategoryActiveTrueAndNameContainingIgnoreCase(
                    ProductStatus.ACTIVE,
                    categoryId,
                    normalizedKeyword.get(),
                    pageRequest
            );
        }
        if (categoryId != null) {
            return productRepository.findByStatusAndCategoryIdAndCategoryActiveTrue(
                    ProductStatus.ACTIVE,
                    categoryId,
                    pageRequest
            );
        }
        return normalizedKeyword
                .map(value -> productRepository.findByStatusAndCategoryActiveTrueAndNameContainingIgnoreCase(
                        ProductStatus.ACTIVE,
                        value,
                        pageRequest
                ))
                .orElseGet(() -> productRepository.findByStatusAndCategoryActiveTrue(ProductStatus.ACTIVE, pageRequest));
    }

    private ProductEntity getRequiredEntity(UUID productId) {
        return productRepository.findWithCategoryById(productId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PRODUCT_NOT_FOUND));
    }

    private ProductEntity getRequiredEntityForUpdate(UUID productId) {
        return productRepository.findLockedWithCategoryById(productId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PRODUCT_NOT_FOUND));
    }

    private void validateUniqueProduct(String sku, String slug) {
        if (productRepository.existsBySkuIgnoreCase(sku) || productRepository.existsBySlug(slug)) {
            throw new BusinessException(ResponseCode.PRODUCT_ALREADY_EXISTS);
        }
    }

    private void validateUniqueProduct(UUID productId, String sku, String slug) {
        boolean skuExists = productRepository.existsBySkuIgnoreCaseAndIdNot(sku, productId);
        boolean slugExists = productRepository.existsBySlugAndIdNot(slug, productId);
        if (skuExists || slugExists) {
            throw new BusinessException(ResponseCode.PRODUCT_ALREADY_EXISTS);
        }
    }

    private ProductEntity buildProduct(ProductRequest request, CategoryEntity category, String slug) {
        Instant now = Instant.now();
        return ProductEntity.builder()
                .name(request.name())
                .sku(request.sku())
                .slug(slug)
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .status(resolveStatus(request.status()))
                .category(category)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void updateProduct(ProductEntity product, ProductRequest request, CategoryEntity category, String slug) {
        product.setName(request.name());
        product.setSku(request.sku());
        product.setSlug(slug);
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setStatus(resolveStatus(request.status()));
        product.setCategory(category);
        product.setUpdatedAt(Instant.now());
    }

    private void validateStock(ProductEntity product, int quantity) {
        if (product.getStockQuantity() < quantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
    }

    private PageResponse<ProductResponse> toPageResponse(Page<ProductEntity> page) {
        return new PageResponse<>(
                page.getContent().stream().map(productMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Optional<String> normalizeKeyword(String keyword) {
        return Optional.ofNullable(keyword)
                .map(String::trim)
                .filter(value -> !value.isBlank());
    }

    private ProductStatus resolveStatus(ProductStatus status) {
        return status == null ? ProductStatus.ACTIVE : status;
    }

    public boolean isActive(ProductEntity product) {
        return product.getStatus() == ProductStatus.ACTIVE && product.getCategory().isActive();
    }

    private int resolvePage(Integer page) {
        return page == null || page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
    }

    private int resolveSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private int resolveStockThreshold(Integer threshold) {
        return threshold == null || threshold < DEFAULT_PAGE ? DEFAULT_PAGE : threshold;
    }
}
