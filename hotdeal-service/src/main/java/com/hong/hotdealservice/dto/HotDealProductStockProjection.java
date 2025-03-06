package com.hong.hotdealservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotDealProductStockProjection {
    private Long getId;
    private Integer getStock;
}
