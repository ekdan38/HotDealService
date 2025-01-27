package com.hong.productservice.web.controller;

import com.hong.common.dto.ProductCommonDto;
import com.hong.productservice.dto.product.ProductDto;
import com.hong.productservice.service.product.ProductApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product-service")
public class ProductApiController {

    private final ProductApiService productApiService;

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
