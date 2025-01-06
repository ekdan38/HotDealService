package com.hong.productservice.dto.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {

    private Long id;
    private String title;
    private Long parentId;
    private List<CategoryResponseDto> childCategories;

    public CategoryResponseDto(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public CategoryResponseDto(Long id, String title, Long parentId) {
        this.id = id;
        this.title = title;
        this.parentId = parentId;
    }


    public CategoryResponseDto(Long id, String title, List<CategoryResponseDto> childCategories) {
        this.id = id;
        this.title = title;
        this.childCategories = childCategories;
    }
}
