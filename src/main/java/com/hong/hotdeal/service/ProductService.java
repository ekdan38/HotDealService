package com.hong.hotdeal.service;

import com.hong.hotdeal.domain.Category;
import com.hong.hotdeal.domain.Product;
import com.hong.hotdeal.dto.ProductCategoryDto;
import com.hong.hotdeal.exception.ErrorCode;
import com.hong.hotdeal.exception.custom.ProductException;
import com.hong.hotdeal.repository.ProductRepository;
import com.hong.hotdeal.web.dto.request.product.ProductRequestDto;
import com.hong.hotdeal.web.dto.response.product.ProductPagingResponseDto;
import com.hong.hotdeal.web.dto.response.product.ProductResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryProductService categoryProductService;
    private final CategoryService categoryService;

    // 상품 생성
    @Transactional
    public Long createProduct(ProductRequestDto requestDto) {
        // 저장 되어있는 상품인지 확인
        if (productRepository.existsByTitle(requestDto.getTitle())) {
            throw new ProductException(ErrorCode.PRODUCT_EXISTS);
        }

        // 상품 Entity 생성 && 저장
        Product product = Product.create(requestDto.getTitle(), requestDto.getPrice(), requestDto.getStock());
        Product savedProduct = productRepository.save(product);

        // 카테고리 조회
        Category foundCategory = categoryService.getCategory(requestDto.getCategoryId());

        // 상품에 카테고리 설정 && 저장
        categoryProductService.setCategoryAndSave(foundCategory, product);

        return savedProduct.getId();
    }

    // 상품 페이징
    public ProductPagingResponseDto getProducts(Long cursor, int size, Long categoryId, String search) {
        PageRequest pageRequest = PageRequest.of(0, size);
        // 조회
        Page<Product> page = productRepository.findProductsByCursorAndRequest(cursor, categoryId, search, pageRequest);

        // Dto 변환
        List<ProductResponseDto> list = page.getContent().stream().map(ProductResponseDto::new).collect(Collectors.toList());

        // cursor 갱신
        Long newCursor = list.get(list.size() - 1).getId();

        return new ProductPagingResponseDto(newCursor, list);
    }

    // 상품 상세 정보
    public ProductResponseDto getProduct(Long productId) {
        return categoryProductService.getProductAndCategoryToDto(productId);
    }

    // 상품 수정
    @Transactional
    public Long updateProduct(Long productId, ProductRequestDto requestDto) {
        // 조회
        ProductCategoryDto dto = categoryProductService.getProductAndCategory(productId);
        Product foundProduct = dto.getProduct();
        // 기존 categoryId
        Category category = dto.getCategory();

        // 상품 수정
        foundProduct.update(requestDto.getTitle(), requestDto.getPrice(), requestDto.getStock());

        // 카테고리 수정

        // 새로운 categoryId
        Long newCategoryId = requestDto.getCategoryId();

        // 새로운 카테고리가 존재하는지 확인
        Category newCategory = categoryService.getCategory(newCategoryId);

        categoryProductService.updateProductCategory(category, foundProduct, newCategory);

        return foundProduct.getId();
    }

    // 상품 삭제
    @Transactional
    public void deleteProduct(Long productId) {
        Product foundProduct = findById(productId);
        productRepository.deleteById(foundProduct.getId());
    }

    // 상품 조회
    public Product findById(Long productId){
        return productRepository.findById(productId).orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
