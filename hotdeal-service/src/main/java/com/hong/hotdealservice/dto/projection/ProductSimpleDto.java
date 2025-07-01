package com.hong.hotdealservice.dto.projection;

import com.hong.hotdealservice.dto.ProductCacheDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSimpleDto {

    private Long id;
    private Long hotDealId;
    private BigDecimal price;
    private String title;

    public ProductSimpleDto(ProductCacheDto cachedData) {
        id = cachedData.getProductId();
        hotDealId = cachedData.getHotDealId();
        price = cachedData.getPrice();
        title = cachedData.getTitle();
    }
}

