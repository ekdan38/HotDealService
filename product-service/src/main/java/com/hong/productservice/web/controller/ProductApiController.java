package com.hong.productservice.web.controller;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.productservice.service.product.ProductApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product-service")
public class ProductApiController {

    private final ProductApiService productApiService;

    @GetMapping("/products")
    ResponseEntity<List<ProductCommonDto>> getProductsById(@RequestParam List<Long> productIds) {

        List<ProductCommonDto> productsByIds = productApiService.getProductsByIds(productIds);
        return ResponseEntity.ok().body(productsByIds);
    }

    @PostMapping("/products")
    ResponseEntity<List<ProductCommonDto>> fetchProducts(@RequestBody List<ProductCommonDto> requestDtos) {
        List<ProductCommonDto> resultDtos = productApiService.fetchProducts(requestDtos);
        return ResponseEntity.ok().body(resultDtos);
    }

    @PostMapping("/decrease-stock")
    ResponseEntity<List<ProductStockUpdateResponseDto>> decreaseStock(@RequestBody List<ProductStockUpdateRequestDto> requestDtos) {
        List<ProductStockUpdateResponseDto> resultDtos = productApiService.decreaseStock(requestDtos);
        return ResponseEntity.ok().body(resultDtos);
    }

    @PostMapping("/increase-stock")
    ResponseEntity<List<ProductStockUpdateResponseDto>> increaseStock(@RequestBody List<ProductStockUpdateRequestDto> requestDtos) {
        List<ProductStockUpdateResponseDto> resultDtos = productApiService.increaseStock(requestDtos);
        return ResponseEntity.ok().body(resultDtos);
    }
}
