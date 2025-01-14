package com.hong.productservice.web.controller;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.dto.ProductStockDto;
import com.hong.productservice.service.product.ProductApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    ResponseEntity<Boolean> decreaseStock(@RequestBody List<ProductStockDto> productStockDtos) {
        Boolean result = productApiService.decreaseStock(productStockDtos);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/products/increase-stock")
    ResponseEntity<Boolean> increaseStock(@RequestBody List<ProductStockDto> productStockDtos) {
        Boolean result = productApiService.increaseStock(productStockDtos);
        return ResponseEntity.ok().body(result);
    }
}
