package com.hong.hotdealservice.domain;

import com.hong.hotdealservice.domain.base.TimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RedisStockSaveFail extends TimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long hotDealProductId;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer stock;

    @Lob
    @Column(nullable = false)
    private String reason;

    private RedisStockSaveFail(Long hotDealProductId, LocalDateTime endTime, Integer stock, String reason) {
        this.hotDealProductId = hotDealProductId;
        this.endTime = endTime;
        this.stock = stock;
        this.reason = reason;
    }

    // == 생성 메서드 ==
    public static RedisStockSaveFail create(Long hotDealProductId, LocalDateTime endTime, Integer stock, String reason){
        return new RedisStockSaveFail(hotDealProductId, endTime, stock, reason);
    }
}
