package com.hong.hotdeal.service;

import com.hong.hotdeal.domain.Category;
import com.hong.hotdeal.exception.ErrorCode;
import com.hong.hotdeal.exception.custom.CategoryException;
import com.hong.hotdeal.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j(topic = "[CategoryService]")
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // category 조회
    public Category getCategory(Long categoryId){
        return categoryRepository.findById(categoryId).orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_EXISTS));
    }
}
