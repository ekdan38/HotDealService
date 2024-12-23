package com.hong.hotdeal.web.controller;

import com.hong.hotdeal.service.ProductService;
import com.hong.hotdeal.web.dto.request.product.ProductRequestDto;
import com.hong.hotdeal.web.dto.response.ResponseDto;
import com.hong.hotdeal.web.dto.response.product.ProductResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[ProductController]")
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    // 상품 생성
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody @Validated ProductRequestDto requestDto,
                                           BindingResult bindingResult) {
        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("상품 생성 요청 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        Long productId = productService.createProduct(requestDto);

        // 응답 설정
        HashMap<String, Long> data = new HashMap<>();
        data.put("productId", productId);
        ResponseDto<HashMap<String, Long>> responseDto = new ResponseDto<>("상품 생성 성공.", data);
        return ResponseEntity.ok().body(responseDto);
    }

    // 상품 페이징 조회
    @GetMapping
    public ResponseEntity<ResponseDto<List<ProductResponseDto>>> getProducts(@RequestParam(required = false) Long cursor,
                                                                             @RequestParam(defaultValue = "10") int size,
                                                                             @RequestParam(required = false) Long categoryId,
                                                                             @RequestParam(required = false) String search) {
        if (cursor == null) {
            cursor = Long.MAX_VALUE;
        }
        List<ProductResponseDto> result = productService.getProducts(cursor, size, categoryId, search);

        // 응답 설정
        ResponseDto<List<ProductResponseDto>> responseDto = new ResponseDto<>("상품 조회 성공.", result);
        return ResponseEntity.ok().body(responseDto);
    }

    // 상품 단건 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ResponseDto<ProductResponseDto>> getProduct(@PathVariable("productId") Long productId) {

        ProductResponseDto result = productService.getProduct(productId);

        // 응답 설정
        ResponseDto<ProductResponseDto> responseDto = new ResponseDto<>("상품 조회 성공.", result);
        return ResponseEntity.ok().body(responseDto);
    }

    // 상품 수정
    @PutMapping("/{productId}")
    public ResponseEntity<ResponseDto<HashMap<String, Long>>> updateProduct(@PathVariable("productId") Long productId,
                                                                            @RequestBody @Validated ProductRequestDto requestDto) {

        Long id = productService.updateProduct(productId, requestDto);

        // 응답 설정
        HashMap<String, Long> data = new HashMap<>();
        data.put("productId", id);
        ResponseDto<HashMap<String, Long>> responseDto = new ResponseDto<>("상품 수정 성공.", data);
        return ResponseEntity.ok().body(responseDto);
    }

    // 상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<ResponseDto<HashMap<String, Long>>> deleteProduct(@PathVariable("productId") Long productId) {

        productService.deleteProduct(productId);

        // 응답 설정
        HashMap<String, Long> data = new HashMap<>();
        data.put("productId", productId);
        ResponseDto<HashMap<String, Long>> responseDto = new ResponseDto<>("상품 삭제 성공.", data);
        return ResponseEntity.ok().body(responseDto);
    }
}
