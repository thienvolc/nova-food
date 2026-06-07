package com.nova.food.domain.order.policy;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.order.constant.OrderStatus;
import com.nova.food.domain.order.dto.request.CreateOrderRequest;
import com.nova.food.domain.order.entity.OrderEntity;
import com.nova.food.domain.restaurant.constant.RestaurantStatus;
import com.nova.food.domain.restaurant.entity.RestaurantEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderPolicyService {

    public void validateCreateItems(List<CreateOrderRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ResponseCode.ORDER_EMPTY_ITEMS);
        }
        if (items.stream().anyMatch(item -> item.quantity() <= 0)) {
            throw new BusinessException(ResponseCode.ORDER_INVALID_ITEM_QUANTITY);
        }
    }

    public void requireStatus(OrderEntity order, OrderStatus expectedStatus) {
        if (order.getStatus() != expectedStatus) {
            throw new BusinessException(ResponseCode.ORDER_INVALID_STATUS);
        }
    }

    public void validateCustomerCanCancel(OrderEntity order) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ResponseCode.ORDER_CANCELLATION_NOT_ALLOWED);
        }
    }

    public void validateRestaurantCanCancel(OrderEntity order) {
        if (order.getStatus() == OrderStatus.READY_FOR_DELIVERY
                || order.getStatus() == OrderStatus.DELIVERING
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ResponseCode.ORDER_CANCELLATION_NOT_ALLOWED);
        }
    }

    public void validateRestaurantActive(RestaurantEntity restaurant) {
        if (restaurant.getStatus() != RestaurantStatus.ACTIVE) {
            throw new BusinessException(ResponseCode.RESTAURANT_INACTIVE);
        }
    }
}
