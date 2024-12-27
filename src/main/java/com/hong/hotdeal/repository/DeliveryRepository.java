package com.hong.hotdeal.repository;

import com.hong.hotdeal.domain.Delivery;
import com.hong.hotdeal.domain.status.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // 배송 상태 변경 (DELIVERING) 벌크 업데이트
    @Modifying
    @Query("UPDATE Delivery d SET d.status = 'DELIVERING' " +
            "WHERE d.status ='PENDING' AND d.createdAt <= :oneDayAgo")
    void updateStatusToDelivering(@Param("oneDayAgo") LocalDateTime oneDayAgo);

    // 배송 상태 변경 (SHIPPED) 벌크 업데이트
    @Modifying
    @Query("UPDATE Delivery d SET d.status = 'DELIVERED', d.completedAt = :now " +
            "WHERE d.status ='DELIVERING' AND d.createdAt <= :twoDaysAgo")
    void updateStatusToShipped(@Param("twoDaysAgo") LocalDateTime twoDaysAgo,
                               @Param("now") LocalDateTime now);


    //
    @Query("SELECT d " +
            "FROM Delivery d " +
            "JOIN FETCH d.order o " +
            "JOIN FETCH o.orderProducts op " +
            "JOIN FETCH op.product p " +
            "WHERE d.status = 'RETURNING' AND d.returnCreatedAt <= :oneDayAgo")
    List<Delivery> findAllWithOrderAndProductsByStatusAndUpdatedAtBefore(@Param("oneDayAgo") LocalDateTime oneDayAgo);

}
