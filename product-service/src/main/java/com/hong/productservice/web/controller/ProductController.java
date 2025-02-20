package com.hong.productservice.web.controller;

import com.hong.common.dto.ResponseDto;
import com.hong.productservice.dto.category.CategoryDto;
import com.hong.productservice.dto.product.*;
import com.hong.productservice.service.product.ProductApiService;
import com.hong.productservice.service.product.ProductService;
import com.hong.productservice.web.dto.product.ProductRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[ProductController]")
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ProductApiService productApiService;

    // product 생성
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody @Validated ProductRequestDto requestDto,
                                           BindingResult bindingResult) {
        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("상품 생성 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        // reqeustDto dto로 변환
        List<CategoryDto> categoryDtos = requestDto.getCategoryIds().stream()
                .map(CategoryDto::new)
                .collect(Collectors.toList());
        ProductDto productDto = new ProductDto(requestDto.getTitle(), requestDto.getPrice(), requestDto.getStock(), categoryDtos);

        ProductResponseDto resultDto = productService.createProduct(productDto);

        // 응답 설정
        ResponseDto<ProductResponseDto> responseDto = new ResponseDto<>("상품 생성 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // product 페이징 조회
    @GetMapping
    public ResponseEntity<ResponseDto<ProductPagingResponseDto>> getProducts(@RequestParam(required = false) Long cursor,
                                                                             @RequestParam(required = false, defaultValue = "10") int size,
                                                                             @RequestParam(required = false) Long categoryId,
                                                                             @RequestParam(required = false) String search) {

        ProductPagingResponseDto resultDto = productService.getProducts(search, cursor, size, categoryId);

        // 응답 설정
        ResponseDto<ProductPagingResponseDto> responseDto = new ResponseDto<>("상품 조회 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // product 단건 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ResponseDto<ProductResponseDto>> getProduct(@PathVariable("productId") Long productId){

        ProductResponseDto resultDto = productService.getProduct(productId);

        // 응답 설정
        ResponseDto<ProductResponseDto> responseDto = new ResponseDto<>("상품 조회 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // product 재고 조회
    @GetMapping("/stock")
    public ResponseEntity<ResponseDto<List<ProductStockDto>>> getProductStock(@RequestParam List<Long> productIds){

        List<ProductStockDto> resultDto = productApiService.getProductStocks(productIds);
        // 응답 설정
        ResponseDto<List<ProductStockDto>> responseDto = new ResponseDto<>("상품 재고 조회 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // product 수정
    @PutMapping("/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable("productId") Long productId,
                                           @RequestBody @Validated ProductRequestDto requestDto,
                                           BindingResult bindingResult){
        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("상품 수정 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }


        // reqeustDto dto로 변환
        List<CategoryDto> categoryDtos = requestDto.getCategoryIds().stream()
                .map(CategoryDto::new)
                .collect(Collectors.toList());
        ProductDto productDto = new ProductDto(requestDto.getTitle(), requestDto.getPrice(), requestDto.getStock(), categoryDtos);

        ProductResponseDto resultDto = productService.updateProduct(productId, productDto);

        // 응답 설정
        ResponseDto<ProductResponseDto> responseDto = new ResponseDto<>("상품 수정 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }


    // product 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<ResponseDto<ProductResponseDto>> deleteProduct(@PathVariable("productId") Long productId){

        ProductResponseDto resultDto = productService.deleteProduct(productId);

        // 응답 설정
        ResponseDto<ProductResponseDto> responseDto = new ResponseDto<>("상품 삭제 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }
}
