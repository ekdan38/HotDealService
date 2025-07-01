package com.hong.hotdealservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.projection.ProductSimpleDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProductResponseDto {

    private Long hotDealId;
    private Long productId;
    private String title;
    private BigDecimal price;
    private Integer stock;

    public ProductResponseDto(ProductSimpleDto hp, Long hotdealId) {
        this.hotDealId = hotdealId;
        this.productId = hp.getId();
        this.title = hp.getTitle();
        this.price = hp.getPrice();
    }

    public ProductResponseDto(ProductSimpleDto hp) {
        this.productId = hp.getId();
        this.title = hp.getTitle();
        this.price = hp.getPrice();
    }

    public ProductResponseDto(HotDealProduct hp, Integer stock) {
        this.hotDealId = hp.getHotDeal().getId();
        this.productId = hp.getId();
        this.title = hp.getTitle();
        this.price = hp.getPrice();
        this.stock = stock;
    }
}
