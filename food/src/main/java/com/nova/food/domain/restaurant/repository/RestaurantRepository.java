package com.nova.food.domain.restaurant.repository;

import com.nova.food.domain.restaurant.constant.RestaurantStatus;
import com.nova.food.domain.restaurant.entity.RestaurantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<RestaurantEntity, UUID> {

    List<RestaurantEntity> findByStatus(RestaurantStatus status);

    Optional<RestaurantEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Query("""
            select r
            from RestaurantEntity r
            where (:status is null or r.status = :status)
              and (
                :keyword is null
                or lower(r.name) like lower(concat('%', :keyword, '%'))
                or lower(r.address) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<RestaurantEntity> search(String keyword, RestaurantStatus status, Pageable pageable);
}
