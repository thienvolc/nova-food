package com.techora.domain.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum ResponseCode {
    BAD_CREDENTIALS("ERR_BAD_CREDENTIALS", "bad_credentials", UNAUTHORIZED),
    USERNAME_NOT_FOUND("ERR_USERNAME_NOT_FOUND", "username.not_found", NOT_FOUND),
    USER_ALREADY_EXISTS("ERR_USER_ALREADY_EXISTS", "user.already_exists", CONFLICT),
    USER_NOT_FOUND("ERR_USER_NOT_FOUND", "user.not_found", NOT_FOUND),
    CATEGORY_ALREADY_EXISTS("ERR_CATEGORY_ALREADY_EXISTS", "category.already_exists", CONFLICT),
    CATEGORY_NOT_FOUND("ERR_CATEGORY_NOT_FOUND", "category.not_found", NOT_FOUND),
    PRODUCT_ALREADY_EXISTS("ERR_PRODUCT_ALREADY_EXISTS", "product.already_exists", CONFLICT),
    PRODUCT_NOT_FOUND("ERR_PRODUCT_NOT_FOUND", "product.not_found", NOT_FOUND),
    PRODUCT_UNAVAILABLE("ERR_PRODUCT_UNAVAILABLE", "product.unavailable", CONFLICT),
    INSUFFICIENT_STOCK("ERR_INSUFFICIENT_STOCK", "product.insufficient_stock", CONFLICT),
    CART_ITEM_NOT_FOUND("ERR_CART_ITEM_NOT_FOUND", "cart.item_not_found", NOT_FOUND),
    CART_EMPTY("ERR_CART_EMPTY", "cart.empty", CONFLICT),
    ORDER_NOT_FOUND("ERR_ORDER_NOT_FOUND", "order.not_found", NOT_FOUND),
    INVALID_ORDER_STATUS_TRANSITION("ERR_INVALID_ORDER_STATUS_TRANSITION", "order.invalid_status_transition", CONFLICT),
    PAYMENT_ALREADY_EXISTS("ERR_PAYMENT_ALREADY_EXISTS", "payment.already_exists", CONFLICT),
    PAYMENT_NOT_FOUND("ERR_PAYMENT_NOT_FOUND", "payment.not_found", NOT_FOUND),
    INVALID_PAYMENT_STATUS_TRANSITION("ERR_INVALID_PAYMENT_STATUS_TRANSITION", "payment.invalid_status_transition", CONFLICT),
    IDEMPOTENCY_KEY_CONFLICT("ERR_IDEMPOTENCY_KEY_CONFLICT", "idempotency.key_conflict", CONFLICT),
    INVALID_TOKEN("ERR_INVALID_TOKEN", "token.invalid", UNAUTHORIZED),
    ACCESS_DENIED("ERR_ACCESS_DENIED", "access_denied", FORBIDDEN),
    INTERNAL_EXCEPTION("ERR_INTERNAL_EXCEPTION", "internal_server_error", INTERNAL_SERVER_ERROR),
    SUCCESS("SUCCESS", "request.ok", OK);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;
}
