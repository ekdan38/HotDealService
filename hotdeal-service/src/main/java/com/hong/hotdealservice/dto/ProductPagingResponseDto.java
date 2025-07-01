package com.hong.hotdealservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPagingResponseDto {

    private Long cursor;
    private Long hotDealId;
    private List<ProductResponseDto> hotDealProducts;
}
