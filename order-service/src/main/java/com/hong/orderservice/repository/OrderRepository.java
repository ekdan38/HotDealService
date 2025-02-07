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

    // 결제 까지 완료한 주문들 조회 (Fetch Join 으로 쿼리 최적화 하기 위해서 List 로 반환 페이징)
    // jpql은 limit 미지원 => pageable 사용 해서 size 적용
    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.orderProducts op " +
            "JOIN FETCH o.delivery " +
            "WHERE o.id < :cursor " +
            "AND o.status = 'PAID' " +
            "AND (:userId IS NULL OR o.userId = :userId) " +
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

    // 주문 환불 처리 update 쿼리
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

    // 특정 user 의 order 를 주문 환불 처리 update 쿼리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Order o " +
            "SET o.status = 'RETURNED' " +
            "WHERE o.status = 'RETURN_REQUESTED' " +
            "AND o.userId = :userId " +
            "AND o.delivery.id IN (" +
            "SELECT d.id " +
            "FROM Delivery d " +
            "WHERE d.deliveryStatus = 'RETURN_REQUESTED' " +
            "AND d.returnStartedAt <= :oneDayAgo)")
    int bulkUpdateOrderStatusToReturnedByUserId(@Param("userId") Long userId,
                                                @Param("oneDayAgo") LocalDateTime oneDayAgo);


    // userId 로 order 조회
    @Query("SELECT o.id " +
            "FROM Order o " +
            "WHERE o.userId = :userId")
    List<Long> findOrdersByUserId(@Param("userId") Long userId);

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
    Optional<Order> findOrderWithOrderProductsByUserIdAndOrderId(@Param("userId") Long userId,
                                                                 @Param("orderId") Long orderId);


}
