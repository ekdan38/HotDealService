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
    private Integer quantity;
    private Integer hotDealPrice;
}
