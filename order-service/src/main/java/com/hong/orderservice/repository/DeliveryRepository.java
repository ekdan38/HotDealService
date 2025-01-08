package com.hong.orderservice.repository;

import com.hong.orderservice.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // 배송 상태 변경 벌크 업데이트
    @Modifying
    @Query("UPDATE Delivery d SET d.status = :newStatus " +
            "WHERE d.status = :status AND d.startedAt <= :dayAgo")
    void updateOrderStatus(@Param("dayAgo") LocalDateTime dayAgo,
                           @Param("newStatus") String newStatus,
                           @Param("status") String status);
}
