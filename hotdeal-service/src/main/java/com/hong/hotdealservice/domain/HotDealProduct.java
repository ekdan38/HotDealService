package com.hong.hotdealservice.domain;

import com.hong.hotdealservice.domain.base.TimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class HotDealProduct extends TimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotdeal_id", nullable = false)
    private HotDeal hotDeal;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    private HotDealProduct(String title, BigDecimal price, Integer stock) {
        this.title = title;
        this.price = price;
        this.stock = stock;
    }

    // == 생성 메서드 ==
    public static HotDealProduct create(String title, BigDecimal price, Integer stock){
       return new HotDealProduct(title, price, stock);
    }

    // == hotDeal 에서 연관 관계 설정에 사용할 메서드 ==
    protected void setHotDeal(HotDeal hotDeal){
        this.hotDeal = hotDeal;
    }

    // == product Field update 메서드 ==
    public void updateFields(String title, BigDecimal price, Integer stock){
        this.title = title;
        this.price = price;
        this.stock = stock;
    }

    // == stock 감소 메서드 ==
    public boolean decreaseStock(Integer quantity) {
        if (this.stock - quantity < 0) return false;
        this.stock -= quantity;
        return true;
    }

    // == stock 조정 메서드 ==
    public void syncStock(Integer stock){
        this.stock = stock;
    }

    // == stock 증가 메서드 ==
    public boolean increaseStock(Integer quantity) {
        this.stock += quantity;
        return true;
    }
}
