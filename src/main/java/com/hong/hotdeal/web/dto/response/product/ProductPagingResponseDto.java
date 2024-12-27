package com.hong.hotdeal.web.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProductPagingResponseDto {

    private Long cursor;

    private List<ProductResponseDto> products;
}
