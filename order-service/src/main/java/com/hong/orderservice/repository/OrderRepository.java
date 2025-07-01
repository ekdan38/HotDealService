package com.hong.orderservice.repository;

import com.hong.orderservice.domain.Order;
import com.hong.orderservice.dto.OrderProductIdDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    Optional<Order> findByIdAndUserId (String id, Long userId);

    @Query("SELECT new com.hong.orderservice.dto.OrderProductIdDto(op.productId)" +
            "FROM Order o " +
            "JOIN o.orderProducts op " +
            "WHERE o.id = :orderId " +
            "AND o.userId = :userId")
    List<OrderProductIdDto> findProductIdsByIdAndUserId(@Param("orderId") String orderId,
                                                        @Param("userId") Long userId);

    // 주문 조회 (Fetch Join 으로 orderProducts, delivery 조회)
    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.orderProducts op " +
            "JOIN FETCH o.delivery " +
            "WHERE o.id = :orderId " +
            "AND o.userId = :userId ")
    Optional<Order> findOrderWithDeliveryAndOpById(@Param("orderId") String orderId,
                                                   @Param("userId") Long userId);

    // user 의 모든 주문 페이징 조회
    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.delivery " +
            "WHERE o.createdAt < :cursor " +
            "AND o.userId = :userId " +
            "ORDER BY o.createdAt DESC")
    List<Order> findOrdersByCursorAndUserIdAndSize(@Param("cursor") LocalDateTime cursor,
                                                    @Param("userId") Long userId,
                                                    Pageable pageable);

    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.delivery d " +
            "WHERE o.status = 'RETURN_REQUESTED' " +
            "AND d.status = 'RETURN_REQUESTED' " +
            "AND d.returnStartedAt < :oneDayAgo")
    Page<Order> findRefundedOrders(@Param("oneDayAgo") LocalDateTime oneDayAgo, Pageable pageable);



    // 환불 처리 후 1일 경과한 order status bulkUpdate
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Order o " +
            "SET o.status = 'RETURNED' " +
            "WHERE o.status = 'RETURN_REQUESTED' " +
            "AND o.delivery.id IN (" +
            "SELECT d.id " +
            "FROM Delivery d " +
            "WHERE d.status = 'RETURN_REQUESTED' " +
            "AND d.returnStartedAt <= :oneDayAgo)")
    int bulkUpdateOrderStatusToReturned(@Param("oneDayAgo") LocalDateTime oneDayAgo);

    // userId, orderId 로 order 조회 (Fetch Join Delivery)
    @Query("SELECT o " +
            "FROM Order o " +
            "JOIN FETCH o.delivery d " +
            "WHERE o.id = :orderId")
    Optional<Order> findByIdAndUserIdWithDelivery(@Param("orderId") String orderId);


    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.delivery " +
            "WHERE o.status = 'PENDING_PAYMENT' " +
            "AND o.expiresAt <= :now " +
            "ORDER BY o.id ASC")
    List<Order> findExpiredPendingOrders(@Param("now") LocalDateTime now);
}
