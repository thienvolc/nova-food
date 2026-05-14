package com.techora.domain.product.repository;

import com.techora.domain.product.constant.ProductStatus;
import com.techora.domain.product.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySlug(String slug);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findWithCategoryById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from ProductEntity product join fetch product.category where product.id = :id")
    Optional<ProductEntity> findLockedWithCategoryById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findWithCategoryBySlug(String slug);

    @EntityGraph(attributePaths = "category")
    Page<ProductEntity> findByStatusAndCategoryActiveTrue(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<ProductEntity> findByStatusAndCategoryIdAndCategoryActiveTrue(
            ProductStatus status,
            UUID categoryId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    Page<ProductEntity> findByStatusAndCategoryActiveTrueAndNameContainingIgnoreCase(
            ProductStatus status,
            String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    Page<ProductEntity> findByStatusAndCategoryIdAndCategoryActiveTrueAndNameContainingIgnoreCase(
            ProductStatus status,
            UUID categoryId,
            String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    Page<ProductEntity> findByStockQuantityLessThanEqual(int stockThreshold, Pageable pageable);
}
