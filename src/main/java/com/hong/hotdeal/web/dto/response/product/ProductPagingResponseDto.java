package com.hong.hotdeal.web.dto.response.product;

import lombok.Data;

import java.util.List;

@Data
public class ProductPagingResponseDto {

    private Long cursor;

    private List<ProductResponseDto> list;

    public ProductPagingResponseDto(Long cursor, List<ProductResponseDto> list) {
        this.cursor = cursor;
        this.list = list;
    }
}
