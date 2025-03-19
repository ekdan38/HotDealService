package com.hong.hotdealservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HotDealProductStockDto {

    private Long hotDealProductId;
    private Long productId;
    private String productTitle;
    private Integer stock;
    private Integer hotDealPrice;

    public HotDealProductStockDto(Long hotDealProductId, Integer stock) {
        this.hotDealProductId = hotDealProductId;
        this.stock = stock;
    }
}
