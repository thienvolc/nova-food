package com.techora.domain.cart.mapper;

import com.techora.domain.cart.dto.response.CartItemResponse;
import com.techora.domain.cart.dto.response.CartResponse;
import com.techora.domain.cart.entity.CartEntity;
import com.techora.domain.cart.entity.CartItemEntity;
import com.techora.domain.product.entity.ProductEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartMapper {
    public CartResponse toResponse(CartEntity cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new CartResponse(
                cart.getId(),
                cart.getUser().getId(),
                items,
                calculateTotal(items),
                cart.getUpdatedAt()
        );
    }

    private CartItemResponse toItemResponse(CartItemEntity item) {
        ProductEntity product = item.getProduct();
        BigDecimal subtotal = calculateSubtotal(product.getPrice(), item.getQuantity());

        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getSlug(),
                product.getPrice(),
                item.getQuantity(),
                subtotal
        );
    }

    private BigDecimal calculateTotal(List<CartItemResponse> items) {
        return items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateSubtotal(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
