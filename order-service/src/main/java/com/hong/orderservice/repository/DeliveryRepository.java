package com.hong.orderservice.repository;

import com.hong.orderservice.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // 주문 후 1일 경과한 배송 상태 변경 (DELIVERING)
    // 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.status = 'DELIVERING', d.startedAt = :startedAt " +
            "WHERE d.status = 'PENDING' " +
            "AND d.createdAt <= :dayAgo")
    int bulkUpdatePendingDeliveriesToDelivering(@Param("dayAgo") LocalDateTime dayAgo,
                                                @Param("startedAt") LocalDateTime startedAt);

    // 배송 후 1일 경과한 배송 상태 변경 (DELIVERED)
    // 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.status = 'DELIVERED', d.completedAt = :completedAt " +
            "WHERE d.status = 'DELIVERING' " +
            "AND d.startedAt <= :dayAgo")
    int bulkUpdateDeliveringDeliveriesToDelivered(@Param("dayAgo") LocalDateTime datAgo,
                                                  @Param("completedAt") LocalDateTime completedAt);


    // 특정 user 의 order 를 주문 후 1일 경과한 배송 상태 변경 (DELIVERING)
    // 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.status = 'DELIVERING', d.startedAt = :startedAt " +
            "WHERE d.status = 'PENDING' " +
            "AND d.createdAt <= :dayAgo " +
            "AND d.order.id IN :orderIds")
    int bulkUpdatePendingDeliveriesToDeliveringByOrderIds(@Param("orderIds") List<Long> orderIds,
                                                          @Param("dayAgo") LocalDateTime dayAgo,
                                                          @Param("startedAt") LocalDateTime startedAt);


    // 특정 user 의 order 를 배송 후 1일 경과한 배송 상태 변경 (DELIVERED)
    // 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Delivery d " +
            "SET d.status = 'DELIVERED', d.completedAt = :completedAt " +
            "WHERE d.status = 'DELIVERING' " +
            "AND d.startedAt <= :dayAgo " +
            "AND d.order.id IN :orderIds")
    int bulkUpdateDeliveringDeliveriesToDeliveredByOrderIds(@Param("orderIds") List<Long> orderIds,
                                                            @Param("dayAgo") LocalDateTime dayAgo,
                                                            @Param("completedAt") LocalDateTime completedAt);
}
