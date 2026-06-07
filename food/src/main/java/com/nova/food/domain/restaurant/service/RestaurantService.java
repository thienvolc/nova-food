package com.nova.food.domain.restaurant.service;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import com.nova.food.domain.common.dto.PageResponse;
import com.nova.food.domain.restaurant.constant.RestaurantStatus;
import com.nova.food.domain.restaurant.dto.request.CreateRestaurantRequest;
import com.nova.food.domain.restaurant.dto.request.UpdateRestaurantRequest;
import com.nova.food.domain.restaurant.dto.response.RestaurantResponse;
import com.nova.food.domain.restaurant.entity.RestaurantEntity;
import com.nova.food.domain.restaurant.mapper.RestaurantMapper;
import com.nova.food.domain.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;

    @Transactional
    public RestaurantResponse create(CreateRestaurantRequest request, UUID ownerId) {
        Instant now = Instant.now();
        RestaurantEntity restaurant = RestaurantEntity.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name(request.name())
                .description(request.description())
                .address(request.address())
                .phone(request.phone())
                .status(RestaurantStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> listActive() {
        return restaurantRepository.findByStatus(RestaurantStatus.ACTIVE).stream()
                .map(restaurantMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<RestaurantResponse> search(String keyword,
                                                   RestaurantStatus status,
                                                   int page,
                                                   int size,
                                                   boolean canFilterByStatus) {
        validatePage(page, size);
        validateStatusFilter(status, canFilterByStatus);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = restaurantRepository.search(clean(keyword), resolveStatus(status), pageable)
                .map(restaurantMapper::toResponse);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public RestaurantResponse get(UUID restaurantId) {
        return restaurantMapper.toResponse(getRequiredRestaurant(restaurantId));
    }

    @Transactional
    public RestaurantResponse update(UUID restaurantId, UpdateRestaurantRequest request, UUID userId, boolean admin) {
        RestaurantEntity restaurant = getRequiredRestaurant(restaurantId);
        validateCanManage(restaurant, userId, admin);
        restaurant.update(request.name(), request.description(), request.address(), request.phone());
        return restaurantMapper.toResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse updateStatus(UUID restaurantId, RestaurantStatus status, UUID userId, boolean admin) {
        RestaurantEntity restaurant = getRequiredRestaurant(restaurantId);
        validateCanManage(restaurant, userId, admin);
        restaurant.updateStatus(status);
        return restaurantMapper.toResponse(restaurant);
    }

    @Transactional(readOnly = true)
    public RestaurantEntity getRequiredRestaurant(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESTAURANT_NOT_FOUND));
    }

    public void validateCanManage(RestaurantEntity restaurant, UUID userId, boolean admin) {
        if (!admin && !restaurant.getOwnerId().equals(userId)) {
            throw new BusinessException(ResponseCode.ACCESS_DENIED);
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessException(ResponseCode.INVALID_PAGE_REQUEST);
        }
    }

    private void validateStatusFilter(RestaurantStatus status, boolean canFilterByStatus) {
        if (status != null && !canFilterByStatus) {
            throw new BusinessException(ResponseCode.ACCESS_DENIED);
        }
    }

    private RestaurantStatus resolveStatus(RestaurantStatus status) {
        if (status == null) {
            return RestaurantStatus.ACTIVE;
        }
        return status;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
