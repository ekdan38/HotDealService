package com.hong.productservice.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockDto {

    private Long productId;
    private String title;
    private Integer price;
    private Integer stock;

    public ProductStockDto(Long productId, String title, Integer price) {
        this.productId = productId;
        this.title = title;
        this.price = price;
    }
}
