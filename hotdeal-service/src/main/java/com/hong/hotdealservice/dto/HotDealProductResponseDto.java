package com.hong.hotdealservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hong.hotdealservice.domain.HotDealProduct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HotDealProductResponseDto {

    private Long hotDealId;
    private Long hotDealProductId;
    private Long originalProductId;
    private String productTitle;
    private Integer originalPrice;
    private Integer hotDealPrice;
    private Double discountRate;
    private Integer stock;

    public HotDealProductResponseDto(HotDealProduct hp) {
        this.hotDealProductId = hp.getId();
        this.originalProductId = hp.getProductId();
        this.productTitle = hp.getProductTitle();
        this.originalPrice = hp.getOriginalPrice();
        this.hotDealPrice = hp.getHotDealPrice();
        this.discountRate = hp.getDiscountRate();
    }

    public HotDealProductResponseDto(HotDealProduct hp, Integer stock) {
        this.hotDealProductId = hp.getId();
        this.originalProductId = hp.getProductId();
        this.productTitle = hp.getProductTitle();
        this.originalPrice = hp.getOriginalPrice();
        this.hotDealPrice = hp.getHotDealPrice();
        this.discountRate = hp.getDiscountRate();
        this.stock = stock;
    }
}
