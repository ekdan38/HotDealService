package com.hong.productservice.web.controller;

import com.hong.common.dto.ProductCommonDto;
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

    @PostMapping("/products/decrease-stock")
    ResponseEntity<List<ProductCommonDto>> fetchAndDecreaseStock(@RequestBody List<ProductCommonDto> productCommonDtos) {
        List<ProductCommonDto> responseDtos = productApiService.decreaseStock(productCommonDtos);
        return ResponseEntity.ok().body(responseDtos);
    }

    @PostMapping("/products/increase-stock")
    ResponseEntity<List<ProductCommonDto>> fetchAndIncreaseStock(@RequestBody List<ProductCommonDto> productCommonDtos) {
        List<ProductCommonDto> responseDtos = productApiService.increaseStock(productCommonDtos);
        return ResponseEntity.ok().body(responseDtos);
    }
}
