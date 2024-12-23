package com.hong.hotdeal.web.dto.request.product;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductRequestDto {

    private String title;
    // nullable => 카테고리 없으면 unCategorized 로 지정해서 와야됨
    private Long categoryId;
    private Integer price;
    private Integer stock;
}
