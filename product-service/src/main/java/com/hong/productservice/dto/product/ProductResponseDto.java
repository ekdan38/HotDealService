package com.hong.productservice.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hong.productservice.dto.category.CategoryDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProductResponseDto {

    private Long id;
    private String title;
    private Integer price;
    private Integer stock;
    private List<CategoryDto> categories;

    public ProductResponseDto(Long id, String title, Integer price) {
        this.id = id;
        this.title = title;
        this.price = price;
    }

    public ProductResponseDto(Long id, String title, Integer price, List<CategoryDto> categories) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.categories = categories;
    }
}
