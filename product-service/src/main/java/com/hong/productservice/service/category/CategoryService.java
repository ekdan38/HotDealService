package com.hong.productservice.service.category;

import com.hong.productservice.domain.Category;
import com.hong.productservice.dto.category.CategoryDto;
import com.hong.productservice.dto.category.CategoryResponseDto;
import com.hong.productservice.web.dto.cateogry.CategoryRequestDto;

import java.util.List;

public interface CategoryService {

    // 최상위 category 생성
    CategoryResponseDto createCategory(CategoryRequestDto requestDto);

    // 자식 category 생성
    CategoryResponseDto createChildCategory(Long parentCategoryId, CategoryRequestDto requestDto);

    // 전체 category 조회
    List<CategoryResponseDto> getCategories();

    // category 단건 조회(자식 카테고리 포함)
    CategoryResponseDto getCategory(Long categoryId);

    // category 수정(title)
    CategoryResponseDto updateCategory(Long categoryId, CategoryRequestDto requestDto);

    // category 삭제
    CategoryResponseDto deleteCategory(Long categoryId);

    // getCategoriesById
    List<Category> getCategoriesById(List<CategoryDto> categories);

}
