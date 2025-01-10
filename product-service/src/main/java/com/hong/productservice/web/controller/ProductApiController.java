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

    @PostMapping("/products")
    ResponseEntity<List<ProductCommonDto>> getProductsById(@RequestBody List<ProductStockDto> productIds){
        List<Long> ids = productIds.stream()
                .map(ProductStockDto::getProductId)
                .collect(Collectors.toList());

        List<ProductCommonDto> productsByIds = productApiService.getProductsByIds(ids);
        return ResponseEntity.ok().body(productsByIds);
    }

    @PostMapping("/products/decrease-stock")
    ResponseEntity<String> decreaseStock(@RequestBody List<ProductStockDto> productStockDtos){
        productApiService.decreaseStock(productStockDtos);
        return ResponseEntity.ok().body("상품 수량 감소 성공");
    }

    @PostMapping("/products/{productId}/increase-stock")
    void increaseStock(@PathVariable ("productId") Long productId,
                       @RequestParam ("quantity") Integer quantity){
        productApiService.increaseStock(productId, quantity);
    }
}
