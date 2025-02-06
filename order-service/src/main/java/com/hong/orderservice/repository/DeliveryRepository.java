package com.hong.orderservice.repository;

import com.hong.orderservice.domain.Delivery;
import com.hong.orderservice.domain.status.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // 결제 완료, 주문 후 1일 경과한 배송 상태 변경 (DELIVERING)
    // 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.deliveryStatus = :newStatus, d.startedAt = :startedAt " +
            "WHERE d.deliveryStatus = :oldStatus " +
            "AND d.id IN (" +
            "SELECT o.delivery.id " +
            "FROM Order o " +
            "WHERE o.status = 'PAID' " +
            "AND o.paidAt <= :dayAgo)")
    int bulkUpdatePendingDeliveriesToDelivering(@Param("dayAgo") LocalDateTime dayAgo,
                                                @Param("startedAt") LocalDateTime startedAt,
                                                @Param("oldStatus") DeliveryStatus oldStatus,
                                                @Param("newStatus") DeliveryStatus newStatus
    );

    // 결제 완료, 배송 후 1일 경과한 배송 상태 변경 (DELIVERED)
    // 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.deliveryStatus = :newStatus, d.completedAt = :completedAt " +
            "WHERE d.deliveryStatus = :oldStatus " +
            "AND d.startedAt <= :dayAgo " +
            "AND d.id IN (" +
            "SELECT o.delivery.id " +
            "FROM Order o " +
            "WHERE o.status = 'PAID')")
    int bulkUpdateDeliveringDeliveriesToDelivered(@Param("dayAgo") LocalDateTime dayAgo,
                                                  @Param("completedAt") LocalDateTime completedAt,
                                                  @Param("oldStatus") DeliveryStatus oldStatus,
                                                  @Param("newStatus") DeliveryStatus newStatus);


    // 결제 완료, 특정 user 의 order 를 주문 후 1일 경과한 배송 상태 변경 (DELIVERING)
    // 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.deliveryStatus = :newStatus, d.startedAt = :startedAt " +
            "WHERE d.deliveryStatus = :oldStatus " +
            "AND d.id IN (" +
            "SELECT o.delivery.id " +
            "FROM Order o " +
            "WHERE o.userId = :userId " +
            "AND o.status = 'PAID' " +
            "AND o.paidAt <= :dayAgo)")
    int bulkUpdatePendingDeliveriesToDeliveringByUserId(@Param("userId") Long userId,
                                                        @Param("dayAgo") LocalDateTime dayAgo,
                                                        @Param("startedAt") LocalDateTime startedAt,
                                                        @Param("oldStatus") DeliveryStatus oldStatus,
                                                        @Param("newStatus") DeliveryStatus newStatus);


    // 결제 완료, 특정 user 의 order 를 배송 후 1일 경과한 배송 상태 변경 (DELIVERED)
    // 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.deliveryStatus = :newStatus, d.completedAt = :completedAt " +
            "WHERE d.deliveryStatus = :oldStatus " +
            "AND d.startedAt <= :dayAgo " +
            "AND d.id IN (" +
            "SELECT o.delivery.id " +
            "FROM Order o " +
            "WHERE o.userId = :userId " +
            "AND o.status = 'PAID')")
    int bulkUpdateDeliveringDeliveriesToDeliveredByUserId(@Param("userId") Long userId,
                                                          @Param("dayAgo") LocalDateTime dayAgo,
                                                          @Param("completedAt") LocalDateTime completedAt,
                                                          @Param("oldStatus") DeliveryStatus oldStatus,
                                                          @Param("newStatus") DeliveryStatus newStatus);

    // 주문 환불 처리 update 쿼리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.deliveryStatus = 'RETURNED', " +
            "d.returnCompletedAt = :now " +
            "WHERE d.deliveryStatus = 'RETURN_REQUESTED' " +
            "AND d.returnStartedAt <= :oneDayAgo " +
            "AND d.id IN (" +
            "SELECT o.delivery.id " +
            "FROM Order o " +
            "WHERE o.status = 'RETURNED')")
    int bulkUpdateDeliveryStatusReturned(@Param("now") LocalDateTime now,
                                         @Param("oneDayAgo") LocalDateTime oneDayAgo);

    // 특정 user 의 order 를 주문 환불 처리 update 쿼리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.deliveryStatus = 'RETURNED', " +
            "d.returnCompletedAt = :now " +
            "WHERE d.deliveryStatus = 'RETURN_REQUESTED' " +
            "AND d.returnStartedAt <= :oneDayAgo " +
            "AND d.id IN (" +
            "SELECT o.delivery.id " +
            "FROM Order o " +
            "WHERE o.userId = :userId " +
            "AND o.status = 'RETURNED')")
    int bulkUpdateDeliveryStatusReturnedByUserId(@Param("userId") Long userId,
                                                 @Param("now") LocalDateTime now,
                                                 @Param("oneDayAgo") LocalDateTime oneDayAgo);
}
