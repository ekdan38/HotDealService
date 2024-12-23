package com.hong.hotdeal.service;

import com.hong.hotdeal.domain.Category;
import com.hong.hotdeal.domain.CategoryProduct;
import com.hong.hotdeal.domain.Product;
import com.hong.hotdeal.dto.ProductCategoryDto;
import com.hong.hotdeal.exception.ErrorCode;
import com.hong.hotdeal.exception.custom.ProductException;
import com.hong.hotdeal.repository.CategoryProductRepository;
import com.hong.hotdeal.web.dto.response.product.ProductResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryProductService {

    private final CategoryProductRepository categoryProductRepository;

    // 상품과 카테고리 연결하고 저장
    @Transactional
    public Long setCategoryAndSave(Category category, Product product){
        if(categoryProductRepository.existsByCategoryIdAndProductId(category.getId(), product.getId())){
            throw new ProductException(ErrorCode.PRODUCT_EXISTS_CATEGORY);
        }
        CategoryProduct categoryProduct = CategoryProduct.create(category, product);
        CategoryProduct savedCategoryProduct = categoryProductRepository.save(categoryProduct);
        return savedCategoryProduct.getId();
    }

    // 상품의 카테고리 수정
    @Transactional
    public String updateProductCategory(Category category, Product product, Category newCategory){
        CategoryProduct foundCategoryProduct = categoryProductRepository.findByCategoryIdAndProductId(category.getId(), product.getId());
        if(foundCategoryProduct == null){
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        foundCategoryProduct.update(newCategory, product);
        return foundCategoryProduct.getCategory().getTitle();
    }

    // 상품 && 카테고리 join fetch 조회
    public ProductResponseDto getProductAndCategoryToDto(Long productId){
        CategoryProduct foundCategoryProduct = categoryProductRepository.findCategoryProductWithProductAndCategory(productId);
        if(foundCategoryProduct == null){
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Product product = foundCategoryProduct.getProduct();
        Category category = foundCategoryProduct.getCategory();

        return new ProductResponseDto(product, category.getTitle());
    }

    public ProductCategoryDto getProductAndCategory(Long productId){
        CategoryProduct foundCategoryProduct = categoryProductRepository.findCategoryProductWithProductAndCategory(productId);
        if(foundCategoryProduct == null){
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Product product = foundCategoryProduct.getProduct();
        Category category = foundCategoryProduct.getCategory();

        return new ProductCategoryDto(product, category);
    }
}
