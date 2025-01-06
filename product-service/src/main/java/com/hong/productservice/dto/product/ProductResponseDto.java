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
    @JsonProperty("categories")
    private List<CategoryDto> categoryDtos;

    public ProductResponseDto(Long id, String title, Integer price, Integer stock) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.stock = stock;
    }
}
