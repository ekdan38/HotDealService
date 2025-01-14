package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockDto {

    private Long productId;
    private Integer quantity;

    public ProductStockDto(Long productId) {
        this.productId = productId;
    }
}
