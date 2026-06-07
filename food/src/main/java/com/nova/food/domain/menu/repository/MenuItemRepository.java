package com.nova.food.domain.menu.repository;

import com.nova.food.domain.menu.entity.MenuItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItemEntity, UUID> {

    List<MenuItemEntity> findByRestaurantId(UUID restaurantId);

    List<MenuItemEntity> findByRestaurantIdAndAvailableTrue(UUID restaurantId);

    List<MenuItemEntity> findByIdIn(Collection<UUID> ids);

    @Query("""
            select m
            from MenuItemEntity m
            where (:restaurantId is null or m.restaurantId = :restaurantId)
              and (:available is null or m.available = :available)
              and (:minPrice is null or m.price >= :minPrice)
              and (:maxPrice is null or m.price <= :maxPrice)
              and (
                :keyword is null
                or lower(m.name) like lower(concat('%', :keyword, '%'))
                or lower(m.description) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<MenuItemEntity> search(UUID restaurantId,
                                Boolean available,
                                BigDecimal minPrice,
                                BigDecimal maxPrice,
                                String keyword,
                                Pageable pageable);
}
