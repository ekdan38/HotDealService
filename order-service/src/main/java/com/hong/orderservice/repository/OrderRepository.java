package com.hong.orderservice.repository;

import com.hong.orderservice.domain.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Fetch Join 으로 쿼리 최적화 하기 위해서 List 로 반환 페이징
    // jpql은 limit 미지원 => pageable 사용 해서 size 적용
    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.orderProducts op " +
            "JOIN FETCH o.delivery " +
            "WHERE o.id < :cursor " +
            "AND (:userId IS NULL OR o.userId = :userId) " +
            "ORDER BY o.id DESC")
    List<Order> findOrdersByCursorAndUserIdAndSize(@Param("cursor") Long cursor,
                                                   @Param("userId") Long userId,
                                                   Pageable pageable);


    // Fetch Join 으로 orderProducts, delivery 조회
    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.orderProducts op " +
            "JOIN FETCH o.delivery " +
            "WHERE o.id = :orderId " +
            "AND o.userId = :userId")
    Order findOrderByOrderIdAndUserIdWithOpAndD(@Param("orderId") Long orderId,
                                                @Param("userId") Long userId);

    @Query("SELECT o.id " +
            "FROM Order o " +
            "WHERE o.userId = :userId")
    List<Long> findOrdersByUserId(@Param("userId") Long userId);

}
