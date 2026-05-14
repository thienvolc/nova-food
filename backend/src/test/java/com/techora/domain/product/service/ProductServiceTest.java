package com.techora.domain.product.service;

import com.techora.domain.category.dto.request.CategoryRequest;
import com.techora.domain.category.service.CategoryService;
import com.techora.domain.product.constant.ProductStatus;
import com.techora.domain.product.dto.request.ProductRequest;
import com.techora.domain.product.dto.response.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest {
    private static final String CATEGORY_NAME = "Sneakers";
    private static final String PRODUCT_NAME = "Runner Max";
    private static final String PRODUCT_SKU = "RUNNER-MAX-001";
    private static final String DESCRIPTION = "Daily running shoe";
    private static final BigDecimal PRICE = BigDecimal.valueOf(99.99);
    private static final int STOCK_QUANTITY = 10;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Test
    void createPersistsProductWithCategorySnapshot() {
        UUID categoryId = categoryService.create(new CategoryRequest(CATEGORY_NAME, DESCRIPTION, true)).id();
        ProductResponse product = productService.create(productRequest(categoryId));

        assertThat(product.name()).isEqualTo(PRODUCT_NAME);
        assertThat(product.slug()).isEqualTo("runner-max");
        assertThat(product.category().id()).isEqualTo(categoryId);
        assertThat(product.status()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void getPublicProductsOnlyReturnsActiveProducts() {
        UUID categoryId = categoryService.create(new CategoryRequest("Accessories", DESCRIPTION, true)).id();
        ProductResponse activeProduct = productService.create(activeProductRequest(categoryId));
        productService.create(inactiveProductRequest(categoryId));

        assertThat(productService.getPublicProducts(categoryId, null, null, null).items())
                .extracting(ProductResponse::id)
                .containsExactly(activeProduct.id());
    }

    private ProductRequest productRequest(UUID categoryId) {
        return new ProductRequest(
                PRODUCT_NAME,
                PRODUCT_SKU,
                DESCRIPTION,
                PRICE,
                STOCK_QUANTITY,
                categoryId,
                ProductStatus.ACTIVE
        );
    }

    private ProductRequest inactiveProductRequest(UUID categoryId) {
        return new ProductRequest(
                "Runner Hidden",
                "RUNNER-HIDDEN-001",
                DESCRIPTION,
                PRICE,
                STOCK_QUANTITY,
                categoryId,
                ProductStatus.INACTIVE
        );
    }

    private ProductRequest activeProductRequest(UUID categoryId) {
        return new ProductRequest(
                "Runner Visible",
                "RUNNER-VISIBLE-001",
                DESCRIPTION,
                PRICE,
                STOCK_QUANTITY,
                categoryId,
                ProductStatus.ACTIVE
        );
    }
}
