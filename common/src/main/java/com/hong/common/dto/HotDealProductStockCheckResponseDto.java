package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotDealProductStockCheckResponseDto {
    private Long hotDealProductId;
    private String productTitle;
    private Integer requestedQuantity;
    private Integer hotDealPrice;

}
