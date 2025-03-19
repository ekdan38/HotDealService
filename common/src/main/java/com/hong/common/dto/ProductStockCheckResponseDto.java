package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockCheckResponseDto {
    private Long productId;
    private String title;
    private Integer requestedQuantity;
    private Integer price;

}
