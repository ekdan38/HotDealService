package com.hong.orderservice.repository;

import com.hong.orderservice.domain.Delivery;
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
            "SET d.status = 'DELIVERING', d.startedAt = :startedAt " +
            "WHERE d.status = 'DELIVERABLE' " +
            "AND d.id IN (" +
            "SELECT o.delivery.id " +
            "FROM Order o " +
            "WHERE o.status = 'PAID' " +
            "AND o.paidAt <= :dayAgo)")
    int bulkUpdatePendingDeliveriesToDelivering(@Param("dayAgo") LocalDateTime dayAgo,
                                                @Param("startedAt") LocalDateTime startedAt);

    // 결제 완료, 배송 후 1일 경과한 배송 상태 변경 (DELIVERED)
    // 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.status = 'DELIVERED', d.completedAt = :completedAt " +
            "WHERE d.status = 'DELIVERING' " +
            "AND d.startedAt <= :dayAgo " +
            "AND d.id IN (" +
            "SELECT o.delivery.id " +
            "FROM Order o " +
            "WHERE o.status = 'PAID')")
    int bulkUpdateDeliveringDeliveriesToDelivered(@Param("dayAgo") LocalDateTime dayAgo,
                                                  @Param("completedAt") LocalDateTime completedAt);

    // 주문 환불 처리 update 쿼리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.status = 'RETURNED', " +
            "d.returnCompletedAt = :completedAt " +
            "WHERE d.status = 'RETURN_REQUESTED' " +
            "AND d.returnStartedAt <= :dayAgo " +
            "AND d.id IN (" +
            "SELECT o.delivery.id " +
            "FROM Order o " +
            "WHERE o.status = 'RETURN_REQUESTED')")
    int bulkUpdateDeliveryStatusReturned(@Param("dayAgo") LocalDateTime dayAgo,
                                         @Param("completedAt") LocalDateTime completedAt);

}
