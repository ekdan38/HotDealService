package com.hong.orderservice.repository;

import com.hong.orderservice.domain.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 주문 조회 (Fetch Join 으로 orderProducts, delivery 조회)
    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.orderProducts op " +
            "JOIN FETCH o.delivery " +
            "WHERE o.id = :orderId " +
            "AND o.userId = :userId ")
    Optional<Order> findOrderWithDeliveryAndOpById(@Param("orderId") Long orderId,
                                                   @Param("userId") Long userId);

    // user 의 모든 주문 페이징 조회
    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.delivery " +
            "WHERE o.id < :cursor " +
            "AND o.userId = :userId " +
            "ORDER BY o.id DESC")
    List<Order> findOrdersByCursorAndUserIdAndSize(@Param("cursor") Long cursor,
                                                    @Param("userId") Long userId,
                                                    Pageable pageable);

    // 결제 까지 완료한 주문 조회 (Fetch Join 으로 orderProducts, delivery 조회)
    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.orderProducts op " +
            "JOIN FETCH o.delivery " +
            "WHERE o.id = :orderId " +
            "AND o.userId = :userId " +
            "AND o.status = 'PAID'")
    Optional<Order> findPaidOrderByOrderIdAndUserIdWithOpAndD(@Param("orderId") Long orderId,
                                                    @Param("userId") Long userId);

    // 환불 처리 후 1일 경과한 order status bulkUpdate
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Order o " +
            "SET o.status = 'RETURNED' " +
            "WHERE o.status = 'RETURN_REQUESTED' " +
            "AND o.delivery.id IN (" +
            "SELECT d.id " +
            "FROM Delivery d " +
            "WHERE d.deliveryStatus = 'RETURN_REQUESTED' " +
            "AND d.returnStartedAt <= :oneDayAgo)")
    int bulkUpdateOrderStatusToReturned(@Param("oneDayAgo") LocalDateTime oneDayAgo);

    // userId, orderId 로 order 조회 (Fetch Join Delivery)
    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.delivery d " +
            "WHERE o.id = :orderId AND o.userId = :userId")
    Optional<Order> findByIdAndUserIdWithDelivery(@Param("orderId") Long orderId,
                                                  @Param("userId") Long userId);

    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.orderProducts " +
            "WHERE o.userId = :userId " +
            "AND o.id = :orderId")
    Optional<Order> findByOrderIdAndUserIdWithOp(@Param("userId") Long userId,
                                                 @Param("orderId") Long orderId);


}
