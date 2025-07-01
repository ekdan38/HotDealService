package com.hong.hotdealservice.dto;

import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.projection.ProductSimpleDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCacheDto {
    private Long hotDealId;
    private Long productId;
    private String title;
    private BigDecimal price;

    public ProductCacheDto(HotDealProduct hp) {
        this.hotDealId = hp.getHotDeal().getId();
        this.productId = hp.getId();
        this.title = hp.getTitle();
        this.price = hp.getPrice();
    }

    public ProductCacheDto(ProductSimpleDto simpleDto) {
        this.hotDealId = simpleDto.getHotDealId();
        this.productId = simpleDto.getId();
        this.title = simpleDto.getTitle();
        this.price = simpleDto.getPrice();
    }
}
