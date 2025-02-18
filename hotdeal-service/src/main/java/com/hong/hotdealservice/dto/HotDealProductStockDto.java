package com.hong.hotdealservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HotDealProductStockDto {

    private Long hotDealProductId;
    private Long productId;
    private String productTitle;
    private Integer stock;
    private Integer hotDealPrice;
}
