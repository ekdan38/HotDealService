package com.hong.orderservice.repository;

import com.hong.orderservice.domain.Delivery;
import com.hong.orderservice.domain.status.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // 주문 후 1일 경과한 배송 상태 변경 (DELIVERING)
    // 벌크 업데이트
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    @Query("UPDATE Delivery d SET d.status = :newStatus, " +
            "d.startedAt = :startedAt " +
            "WHERE d.status = :oldStatus AND d.createdAt <= :dayAgo")
    void updatePendingDeliveriesToDelivering(@Param("dayAgo") LocalDateTime dayAgo,
                                             @Param("newStatus") DeliveryStatus newStatus,
                                             @Param("startedAt") LocalDateTime startedAt,
                                             @Param("oldStatus") DeliveryStatus oldStatus);

    // 주문 후 1일 경과한 배송 상태 변경 (DELIVERING)
    // 벌크 업데이트
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    @Query("UPDATE Delivery d SET d.status = :newStatus, " +
            "d.createdAt = :completedAt " +
            "WHERE d.status = :oldStatus AND d.createdAt <= :dayAgo")
    void updateDeliveringDeliveriesToDelivered(@Param("dayAgo") LocalDateTime twoDaysAgo,
                                             @Param("newStatus") DeliveryStatus newStatus,
                                             @Param("startedAt") LocalDateTime completedAt,
                                             @Param("oldStatus") DeliveryStatus oldStatus);
}
