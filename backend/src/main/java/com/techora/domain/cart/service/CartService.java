package com.techora.domain.cart.service;

import com.techora.app.aop.BusinessException;
import com.techora.domain.cart.dto.request.AddCartItemRequest;
import com.techora.domain.cart.dto.request.UpdateCartItemRequest;
import com.techora.domain.cart.dto.response.CartResponse;
import com.techora.domain.cart.entity.CartEntity;
import com.techora.domain.cart.entity.CartItemEntity;
import com.techora.domain.cart.mapper.CartMapper;
import com.techora.domain.cart.repository.CartItemRepository;
import com.techora.domain.cart.repository.CartRepository;
import com.techora.domain.common.constant.ResponseCode;
import com.techora.domain.product.entity.ProductEntity;
import com.techora.domain.product.service.ProductService;
import com.techora.domain.user.entity.UserEntity;
import com.techora.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {
    private static final int EMPTY_ITEM_QUANTITY = 0;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final ProductService productService;
    private final UserService userService;

    @Transactional
    public CartResponse getCart(UUID userId) {
        CartEntity cart = getOrCreateCart(userId);
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        CartEntity cart = getOrCreateCart(userId);
        ProductEntity product = productService.getRequiredActiveEntity(request.productId());
        CartItemEntity item = getOrCreateItem(cart, product);

        int requestedQuantity = item.getQuantity() + request.quantity();
        applyQuantity(item, requestedQuantity, product);
        touch(cart);

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItem(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        CartItemEntity item = getRequiredItem(userId, itemId);
        ProductEntity product = item.getProduct();

        validateAvailableProduct(product);
        applyQuantity(item, request.quantity(), product);
        touch(item.getCart());

        return cartMapper.toResponse(cartRepository.save(item.getCart()));
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID itemId) {
        CartItemEntity item = getRequiredItem(userId, itemId);
        CartEntity cart = item.getCart();

        removeCartItem(cart, item);
        touch(cart);

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse clearCart(UUID userId) {
        CartEntity cart = getOrCreateCart(userId);
        clearCart(cart);
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    public CartEntity getRequiredCartWithItems(UUID userId) {
        return getOrCreateCart(userId);
    }

    public void clearCart(CartEntity cart) {
        cart.getItems().clear();
        touch(cart);
    }

    private Optional<CartEntity> findCart(UUID userId) {
        return cartRepository.findWithItemsByUserId(userId);
    }

    private CartEntity getOrCreateCart(UUID userId) {
        return findCart(userId).orElseGet(() -> createCart(userId));
    }

    private CartEntity createCart(UUID userId) {
        Instant now = Instant.now();
        UserEntity user = userService.getRequiredEntity(userId);

        return cartRepository.save(CartEntity.builder()
                .user(user)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private CartItemEntity getOrCreateItem(CartEntity cart, ProductEntity product) {
        return cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> createItem(cart, product));
    }

    private CartItemEntity createItem(CartEntity cart, ProductEntity product) {
        Instant now = Instant.now();
        CartItemEntity item = CartItemEntity.builder()
                .cart(cart)
                .product(product)
                .quantity(EMPTY_ITEM_QUANTITY)
                .createdAt(now)
                .updatedAt(now)
                .build();

        cart.getItems().add(item);
        return item;
    }

    private CartItemEntity getRequiredItem(UUID userId, UUID itemId) {
        return cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CART_ITEM_NOT_FOUND));
    }

    private void applyQuantity(CartItemEntity item, int quantity, ProductEntity product) {
        validateStock(product, quantity);
        item.setQuantity(quantity);
        item.setUpdatedAt(Instant.now());
    }

    private void validateAvailableProduct(ProductEntity product) {
        if (!productService.isActive(product)) {
            throw new BusinessException(ResponseCode.PRODUCT_UNAVAILABLE);
        }
    }

    private void validateStock(ProductEntity product, int requestedQuantity) {
        if (product.getStockQuantity() < requestedQuantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
    }

    private void removeCartItem(CartEntity cart, CartItemEntity item) {
        cart.getItems().remove(item);
    }

    private void touch(CartEntity cart) {
        cart.setUpdatedAt(Instant.now());
    }
}
