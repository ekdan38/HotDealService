package com.hong.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HotDealProductCommonDto {

    private Long hotDealId;
    private Long hotDealProductId;
    private Long productId;
    private String productTitle;
    private Integer quantity;
    private Integer stock;
    private Integer hotDealPrice;

    public HotDealProductCommonDto(Long hotDealId, Long hotDealProductId, Integer quantity) {
        this.hotDealId = hotDealId;
        this.hotDealProductId = hotDealProductId;
        this.quantity = quantity;
    }

    public HotDealProductCommonDto(Long hotDealId, Long hotDealProductId, Long productId, String productTitle, Integer quantity, Integer hotDealPrice) {
        this.hotDealId = hotDealId;
        this.hotDealProductId = hotDealProductId;
        this.productId = productId;
        this.productTitle = productTitle;
        this.quantity = quantity;
        this.hotDealPrice = hotDealPrice;
    }
}
