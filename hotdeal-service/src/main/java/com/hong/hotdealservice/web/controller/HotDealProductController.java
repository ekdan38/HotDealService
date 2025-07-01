package com.hong.hotdealservice.web.controller;

import com.hong.common.dto.ResponseDto;
import com.hong.hotdealservice.dto.ProductResponseDto;
import com.hong.hotdealservice.dto.ProductPagingResponseDto;
import com.hong.hotdealservice.service.HotDealProductServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealProductController]")
@RequestMapping("/hotdeals")
public class HotDealProductController {

    private final HotDealProductServiceImpl hotDealProductService;

    // HotDealProduct 페이징 조회
    @GetMapping("/{hotDealId}/products")
    public ResponseEntity<ResponseDto<ProductPagingResponseDto>> getHotDealProducts(@PathVariable("hotDealId") Long hotDealId,
                                                                                    @RequestParam(required = false) Long cursor,
                                                                                    @RequestParam(required = false, defaultValue = "10") Integer size,
                                                                                    @RequestParam(required = false) String search) {

        ProductPagingResponseDto resultDto = hotDealProductService.getProducts(hotDealId, search, cursor, size);

        // 응답 설정
        ResponseDto<ProductPagingResponseDto> responseDto = new ResponseDto<>("핫딜 상품 페이징 조회 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // HotDealProduct 단건 조회
    @GetMapping("/products/{productId}")
    public ResponseEntity<ResponseDto<ProductResponseDto>> getHotDealProduct(@PathVariable("productId") Long productId) {

        ProductResponseDto resultDto = hotDealProductService.getProduct(productId);

        // 응답 설정
        ResponseDto<ProductResponseDto> responseDto = new ResponseDto<>("핫딜 상품 단건 조회 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }
}
