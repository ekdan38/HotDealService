package com.hong.hotdealservice.domain;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealProductException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Entity
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class HotDealProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hotdeal_product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
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
    private Integer stock;

    private HotDealProduct(Long productId, String productTitle, Integer originalPrice, Double discountRate, Integer stock) {
        this.productId = productId;
        this.productTitle = productTitle;
        this.originalPrice = originalPrice;
        this.discountRate = discountRate;
        this.stock = stock;
    }

    // == 생성 메서드 ==
    public static HotDealProduct create(Long productId, String productTitle, Integer originalPrice, Double discountRate, Integer stock){
        HotDealProduct hotDealProduct = new HotDealProduct(productId, productTitle, originalPrice, discountRate, stock);
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

    // == stock 감소 메서드 ==
    public void decreaseStock(Integer quantity){
        if(this.stock - quantity < 0){
            log.debug("요청 수량보다 재고가 부족합니다. hotDealProductId = {}, 요청 수량 = {}, 재고 수량 = {}", this.id, quantity, this.stock);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_INSUFFICIENT_STOCK, this.id, quantity, this.stock);
        }
        this.stock -= quantity;
        log.info("재고 감소 성공 hotDealProductId = {}, 차감 수량 = {}, 재고 수량 = {}", this.id, quantity, this.stock);
    }

    // == stock 증가 메서드 ==
    public void increaseStock(Integer quantity){
        this.stock += quantity;
        log.info("재고 증가 성공 hotDealProductId = {}, 증가 수량 = {}, 재고 수량 = {}", this.id, quantity, this.stock);
    }


    // == quantity, discountRate update 메서드 ==
    public void updateQuantityAndDiscountRate(int quantity, double discountRate){
        this.stock = quantity;
        this.discountRate = discountRate;
        setHotDealPrice(discountRate);
    }

}
