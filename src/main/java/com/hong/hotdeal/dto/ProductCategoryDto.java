package com.hong.hotdeal.dto;

import com.hong.hotdeal.domain.Category;
import com.hong.hotdeal.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductCategoryDto {

    private Product product;
    private Category category;
}
