package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCommonDto {

    private Long id;
    private String title;
    private Integer price;
    private Integer stock;
    private Integer quantity;
    private Boolean isHotDealProduct;

    public ProductCommonDto(Long id, String title, Integer price, Integer quantity, Boolean isHotDealProduct) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.quantity = quantity;
        this.isHotDealProduct = isHotDealProduct;
    }

    public ProductCommonDto(Long id, String title, Integer price, Integer stock) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.stock = stock;
    }

    public ProductCommonDto(Long id, Integer quantity, Boolean isHotDealProduct) {
        this.id = id;
        this.quantity = quantity;
        this.isHotDealProduct = isHotDealProduct;
    }

    public ProductCommonDto(Long id, Integer quantity) {
        this.id = id;
        this.quantity = quantity;
    }
}
