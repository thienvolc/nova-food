package com.nova.food.domain.delivery.policy;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.delivery.constant.DeliveryStatus;
import com.nova.food.domain.delivery.entity.DeliveryEntity;
import com.nova.food.domain.delivery.repository.DeliveryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class DeliveryPolicyService {
    private final DeliveryRepository deliveryRepository;

    public void validateOrderNotAssigned(UUID orderId) {
        if (deliveryRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ResponseCode.DELIVERY_ALREADY_ASSIGNED);
        }
    }

    public void validateDriverHasNoActiveDelivery(UUID driverId) {
        if (deliveryRepository.existsByDriverIdAndStatusIn(
                driverId,
                List.of(DeliveryStatus.ASSIGNED, DeliveryStatus.DELIVERING)
        )) {
            throw new BusinessException(ResponseCode.DRIVER_HAS_ACTIVE_DELIVERY);
        }
    }

    public void validateAssignedDriver(DeliveryEntity delivery, UUID driverId) {
        if (!delivery.getDriverId().equals(driverId)) {
            throw new BusinessException(ResponseCode.ACCESS_DENIED);
        }
    }

    public void requireStatus(DeliveryEntity delivery, DeliveryStatus status) {
        if (delivery.getStatus() != status) {
            throw new BusinessException(ResponseCode.DELIVERY_INVALID_STATUS);
        }
    }
}
