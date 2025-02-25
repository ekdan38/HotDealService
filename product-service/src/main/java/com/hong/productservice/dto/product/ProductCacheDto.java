package com.hong.productservice.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hong.productservice.dto.category.CategoryDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProductCacheDto {

    private Long id;
    private String title;
    private Integer price;
    private List<CategoryDto> categories;
}
