package com.hong.userservice.repository;

import com.hong.userservice.domain.CartItem;
import com.hong.userservice.dto.CartSimpleDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM CartItem c" +
            " WHERE c.userId = :userId " +
            "AND c.productId IN :productIds")
    int deleteByUserIdAndProductIdIn(@Param("userId") Long userId,
                                     @Param("productIds") List<Long> productIds);

    int countByUserId(Long userId);

    Optional<CartItem>findByUserIdAndProductId(Long userId, Long productId);

}
