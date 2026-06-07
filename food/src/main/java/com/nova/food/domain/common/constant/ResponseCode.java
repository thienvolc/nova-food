package com.nova.food.domain.common.constant;

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
    INVALID_USER_ROLE("ERR_INVALID_USER_ROLE", "user.role.invalid", BAD_REQUEST),
    INVALID_PAGE_REQUEST("ERR_INVALID_PAGE_REQUEST", "page.request.invalid", BAD_REQUEST),
    VALIDATION_FAILED("ERR_VALIDATION_FAILED", "validation.failed", BAD_REQUEST),
    INVALID_REQUEST("ERR_INVALID_REQUEST", "request.invalid", BAD_REQUEST),
    CUSTOMER_PROFILE_NOT_FOUND("ERR_CUSTOMER_PROFILE_NOT_FOUND", "customer.profile.not_found", NOT_FOUND),
    CUSTOMER_PROFILE_ALREADY_EXISTS("ERR_CUSTOMER_PROFILE_ALREADY_EXISTS", "customer.profile.already_exists", CONFLICT),
    DRIVER_PROFILE_NOT_FOUND("ERR_DRIVER_PROFILE_NOT_FOUND", "driver.profile.not_found", NOT_FOUND),
    DRIVER_PROFILE_ALREADY_EXISTS("ERR_DRIVER_PROFILE_ALREADY_EXISTS", "driver.profile.already_exists", CONFLICT),
    INVALID_TOKEN("ERR_INVALID_TOKEN", "token.invalid", UNAUTHORIZED),
    ACCESS_DENIED("ERR_ACCESS_DENIED", "access_denied", FORBIDDEN),
    RESTAURANT_NOT_FOUND("ERR_RESTAURANT_NOT_FOUND", "restaurant.not_found", NOT_FOUND),
    RESTAURANT_INACTIVE("ERR_RESTAURANT_INACTIVE", "restaurant.inactive", BAD_REQUEST),
    MENU_ITEM_NOT_FOUND("ERR_MENU_ITEM_NOT_FOUND", "menu_item.not_found", NOT_FOUND),
    MENU_ITEM_UNAVAILABLE("ERR_MENU_ITEM_UNAVAILABLE", "menu_item.unavailable", BAD_REQUEST),
    INVALID_MENU_ITEM_PRICE("ERR_INVALID_MENU_ITEM_PRICE", "menu_item.price.invalid", BAD_REQUEST),
    MENU_ITEM_INSUFFICIENT_STOCK("ERR_MENU_ITEM_INSUFFICIENT_STOCK", "menu_item.stock.insufficient", BAD_REQUEST),
    INVALID_STOCK_QUANTITY("ERR_INVALID_STOCK_QUANTITY", "menu_item.stock.invalid_quantity", BAD_REQUEST),
    DRIVER_NOT_AVAILABLE("ERR_DRIVER_NOT_AVAILABLE", "driver.not_available", BAD_REQUEST),
    DRIVER_HAS_ACTIVE_DELIVERY("ERR_DRIVER_HAS_ACTIVE_DELIVERY", "driver.active_delivery.exists", CONFLICT),
    ORDER_NOT_FOUND("ERR_ORDER_NOT_FOUND", "order.not_found", NOT_FOUND),
    ORDER_EMPTY_ITEMS("ERR_ORDER_EMPTY_ITEMS", "order.items.empty", BAD_REQUEST),
    ORDER_INVALID_ITEM_QUANTITY("ERR_ORDER_INVALID_ITEM_QUANTITY", "order.item.quantity.invalid", BAD_REQUEST),
    ORDER_ITEMS_MUST_BE_SAME_RESTAURANT("ERR_ORDER_ITEMS_MUST_BE_SAME_RESTAURANT", "order.items.must_be_same_restaurant", BAD_REQUEST),
    ORDER_IDEMPOTENCY_KEY_REUSED("ERR_ORDER_IDEMPOTENCY_KEY_REUSED", "order.idempotency_key.reused", CONFLICT),
    ORDER_INVALID_STATUS("ERR_ORDER_INVALID_STATUS", "order.status.invalid", BAD_REQUEST),
    ORDER_CANCELLATION_NOT_ALLOWED("ERR_ORDER_CANCELLATION_NOT_ALLOWED", "order.cancellation.not_allowed", BAD_REQUEST),
    INVALID_REPORT_DATE_RANGE("ERR_INVALID_REPORT_DATE_RANGE", "report.date_range.invalid", BAD_REQUEST),
    COUPON_NOT_FOUND("ERR_COUPON_NOT_FOUND", "coupon.not_found", NOT_FOUND),
    COUPON_ALREADY_EXISTS("ERR_COUPON_ALREADY_EXISTS", "coupon.already_exists", CONFLICT),
    COUPON_INVALID("ERR_COUPON_INVALID", "coupon.invalid", BAD_REQUEST),
    COUPON_NOT_ACTIVE("ERR_COUPON_NOT_ACTIVE", "coupon.not_active", BAD_REQUEST),
    COUPON_USAGE_LIMIT_REACHED("ERR_COUPON_USAGE_LIMIT_REACHED", "coupon.usage_limit.reached", BAD_REQUEST),
    REVIEW_NOT_FOUND("ERR_REVIEW_NOT_FOUND", "review.not_found", NOT_FOUND),
    REVIEW_ALREADY_EXISTS("ERR_REVIEW_ALREADY_EXISTS", "review.already_exists", CONFLICT),
    REVIEW_NOT_ALLOWED("ERR_REVIEW_NOT_ALLOWED", "review.not_allowed", BAD_REQUEST),
    DELIVERY_NOT_FOUND("ERR_DELIVERY_NOT_FOUND", "delivery.not_found", NOT_FOUND),
    DELIVERY_ALREADY_ASSIGNED("ERR_DELIVERY_ALREADY_ASSIGNED", "delivery.already_assigned", CONFLICT),
    DELIVERY_INVALID_STATUS("ERR_DELIVERY_INVALID_STATUS", "delivery.status.invalid", BAD_REQUEST),
    PAYMENT_NOT_FOUND("ERR_PAYMENT_NOT_FOUND", "payment.not_found", NOT_FOUND),
    PAYMENT_FAILED("ERR_PAYMENT_FAILED", "payment.failed", BAD_REQUEST),
    OUTBOX_EVENT_NOT_FOUND("ERR_OUTBOX_EVENT_NOT_FOUND", "outbox.event.not_found", NOT_FOUND),
    OUTBOX_REPLAY_NOT_ALLOWED("ERR_OUTBOX_REPLAY_NOT_ALLOWED", "outbox.replay.not_allowed", BAD_REQUEST),
    INTERNAL_EXCEPTION("ERR_INTERNAL_EXCEPTION", "internal_server_error", INTERNAL_SERVER_ERROR),
    SUCCESS("SUCCESS", "request.ok", OK);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;
}
