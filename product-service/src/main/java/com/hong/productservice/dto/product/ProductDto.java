package com.hong.productservice.dto.product;

import com.hong.productservice.dto.category.CategoryDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ProductDto {

    private Long id;
    private String title;
    private Integer price;
    private Integer stock;
    private List<CategoryDto> categoryDtos;

    public ProductDto(String title, Integer price, Integer stock, List<CategoryDto> categoryDtos) {
        this.title = title;
        this.price = price;
        this.stock = stock;
        this.categoryDtos = categoryDtos;
    }

    public ProductDto(Long id, String title, Integer stock) {
        this.id = id;
        this.title = title;
        this.stock = stock;
    }
}
