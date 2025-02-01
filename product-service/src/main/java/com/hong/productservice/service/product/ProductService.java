package com.hong.productservice.service.product;

import com.hong.productservice.dto.product.ProductDto;
import com.hong.productservice.dto.product.ProductPagingResponseDto;
import com.hong.productservice.dto.product.ProductResponseDto;

public interface ProductService {

    // product 생성
    ProductResponseDto createProduct(ProductDto requestDto);

    // product 커서 기반 페이징 조회
    ProductPagingResponseDto getProducts(String search, Long cursor, int size, Long categoryId);

    // product 단건 조회
    ProductResponseDto getProduct(Long productId);

    // product 재고만 단건 조회
    ProductResponseDto getProductStock(Long productId);

    // product 수정
    ProductResponseDto updateProduct(Long productId, ProductDto requestDto);

    // product 삭제
    ProductResponseDto deleteProduct(Long productId);

}
