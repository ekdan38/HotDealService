package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockUpdateRequestDto {

    private Long productId;
    private Integer quantity;

}
