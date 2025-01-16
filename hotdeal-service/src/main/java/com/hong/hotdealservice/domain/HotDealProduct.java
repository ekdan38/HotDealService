package com.hong.hotdealservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class HotDealProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hotdeal_product_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hotdeal_id", nullable = false)
    private HotDeal hotDeal;

    @Column(nullable = false)
    private Long productId;

    // 반정규화
    @Column(nullable = false)
    private String productTitle;

    // 반정규화
    @Column(nullable = false)
    private Integer originalPrice;

    @Column(nullable = false)
    private Integer hotDealPrice;

    @Column(nullable = false)
    private Double discountRate;

    @Column(nullable = false)
    private Integer quantity;

    private HotDealProduct(Long productId, String productTitle, Integer originalPrice, Double discountRate, Integer quantity) {
        this.productId = productId;
        this.productTitle = productTitle;
        this.originalPrice = originalPrice;
        this.discountRate = discountRate;
        this.quantity = quantity;
    }

    // == 생성 메서드 ==
    public static HotDealProduct create(Long productId, String productTitle, Integer originalPrice, Double discountRate, Integer quantity){
        HotDealProduct hotDealProduct = new HotDealProduct(productId, productTitle, originalPrice, discountRate, quantity);
        hotDealProduct.setHotDealPrice(discountRate);
        return hotDealProduct;
    }

    // == HotDeal 에서 연관 관계 설정에 사용할 메서드 ==
    protected void setHotDeal(HotDeal hotDeal){
        this.hotDeal = hotDeal;
    }

    // == hotDealPrice 설정 메서드 ==
    private void setHotDealPrice(Double discountRate){
        this.hotDealPrice = (int) Math.floor(this.originalPrice * (1 - discountRate));
    }

    // == HotDealProduct update 메서드 ==
    public void update(int quantity, double discountRate){
        this.quantity = quantity;
        setHotDealPrice(discountRate);
    }

}
