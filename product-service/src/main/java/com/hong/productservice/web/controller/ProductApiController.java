package com.hong.productservice.web.controller;

import com.hong.common.dto.*;
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

    @PostMapping("/products")
    ResponseEntity<List<ProductStockCheckResponseDto>> fetchProducts(@RequestBody List<ProductStockCheckRequestDto> requestDtos) {
        List<ProductStockCheckResponseDto> responseDtos = productApiService.fetchProductAndValidateStock(requestDtos);
        return ResponseEntity.ok().body(responseDtos);
    }

    @PostMapping("/decrease-stock")
    ResponseEntity<List<ProductStockUpdateResponseDto>> decreaseStock(@RequestBody List<ProductStockUpdateRequestDto> requestDtos) {
        List<ProductStockUpdateResponseDto> responseDtos = productApiService.decreaseStock(requestDtos);
        return ResponseEntity.ok().body(responseDtos);
    }

    @PostMapping("/increase-stock")
    ResponseEntity<List<ProductStockUpdateResponseDto>> increaseStock(@RequestBody List<ProductStockUpdateRequestDto> requestDtos) {
        List<ProductStockUpdateResponseDto> responseDtos = productApiService.increaseStock(requestDtos);
        return ResponseEntity.ok().body(responseDtos);
    }
}
