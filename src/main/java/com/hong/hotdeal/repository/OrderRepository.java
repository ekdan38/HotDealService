package com.hong.hotdeal.repository;

import com.hong.hotdeal.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.orderProducts op " +
            "JOIN FETCH op.product p " +
            "JOIN FETCH o.delivery d " +
            "WHERE o.id = :orderId AND o.user.id = :userId ")
    Optional<Order> findOrderWithProductsAndDelivery(@Param("orderId") Long orderId,
                                                     @Param("userId") Long userId);



    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.delivery d " +
            "WHERE o.user.id = :userId " +
            "AND o.id < :cursor " +
            "ORDER BY o.id DESC")
    Page<Order> findOrdersByCursorAndUserId(@Param("cursor") Long cursor,
                                            @Param("userId") Long userId,
                                            @Param("size") int size,
                                            Pageable pageable);

    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.delivery d " +
            "WHERE o.id = :orderId AND o.user.id = :userId")
    Optional<Order>findOrderWithDelivery(@Param("userId") Long userId,
                                          @Param("OrderId") Long orderId);
}
