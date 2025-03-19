package com.hong.hotdealservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HotDealProductCacheDto {
    private Long hotDealId;
    private Long hotDealProductId;
    private Long originalProductId;
    private String productTitle;
    private Integer originalPrice;
    private Integer hotDealPrice;
    private Double discountRate;

    public HotDealProductCacheDto(HotDealProduct hotDealProduct) {
        this.hotDealId = hotDealProduct.getHotDeal().getId();
        this.hotDealProductId = hotDealProduct.getId();
        this.originalProductId = hotDealProduct.getProductId();
        this.productTitle = hotDealProduct.getProductTitle();
        this.originalPrice = hotDealProduct.getOriginalPrice();
        this.hotDealPrice = hotDealProduct.getHotDealPrice();
        this.discountRate = hotDealProduct.getDiscountRate();
    }
}
