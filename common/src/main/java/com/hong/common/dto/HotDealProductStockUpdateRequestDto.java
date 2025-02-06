package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotDealProductStockUpdateRequestDto {

    private Long hotDealId;
    private Long hotDealProductId;
    private Integer quantity;
}
