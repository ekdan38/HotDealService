package com.hong.hotdealservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    public HotDealProductResponseDto(Long hotDealProductId, Long originalProductId, String productTitle, Integer originalPrice, Integer hotDealPrice, Double discountRate) {
        this.hotDealProductId = hotDealProductId;
        this.originalProductId = originalProductId;
        this.productTitle = productTitle;
        this.originalPrice = originalPrice;
        this.hotDealPrice = hotDealPrice;
        this.discountRate = discountRate;
    }

    public HotDealProductResponseDto(Long hotDealProductId, Long originalProductId, String productTitle, Integer originalPrice, Integer hotDealPrice, Double discountRate, Integer stock) {
        this.hotDealProductId = hotDealProductId;
        this.originalProductId = originalProductId;
        this.productTitle = productTitle;
        this.originalPrice = originalPrice;
        this.hotDealPrice = hotDealPrice;
        this.discountRate = discountRate;
        this.stock = stock;
    }
}
