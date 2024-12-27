package com.hong.hotdeal.repository;

import com.hong.hotdeal.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // 배송 상태 변경 (DELIVERING) 벌크 업데이트
    @Modifying
    @Query("UPDATE Delivery d SET d.status = 'DELIVERING' " +
            "WHERE d.status ='PENDING' AND d.createdAt <= :oneDayAgo")
    void updateStatusToDelivering(LocalDateTime oneDayAgo);

    // 배송 상태 변경 (SHIPPED) 벌크 업데이트
    @Modifying
    @Query("UPDATE Delivery d SET d.status = 'SHIPPED' " +
            "WHERE d.status ='DELIVERING' AND d.createdAt <= :twoDaysAgo")
    void updateStatusToShipped(LocalDateTime twoDaysAgo);
}
