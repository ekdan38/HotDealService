package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotDealProductStockCheckResponseDto {
    private Long hotDealId;
    private Long hotDealProductId;
    private Long productId;
    private String productTitle;
    private Integer requestQuantity;
    private Integer hotDealPrice;
    private Integer stock;

    public HotDealProductStockCheckResponseDto(Long hotDealId, Long hotDealProductId, Long productId, String productTitle, Integer requestQuantity, Integer hotDealPrice) {
        this.hotDealId = hotDealId;
        this.hotDealProductId = hotDealProductId;
        this.productId = productId;
        this.productTitle = productTitle;
        this.requestQuantity = requestQuantity;
        this.hotDealPrice = hotDealPrice;
    }
}
