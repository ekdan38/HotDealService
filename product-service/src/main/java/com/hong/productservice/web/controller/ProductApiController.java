package com.hong.productservice.web.controller;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.ProductException;
import com.hong.productservice.domain.Product;
import com.hong.productservice.repository.ProductRepository;
import com.hong.productservice.service.product.ProductApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product-service")
public class ProductApiController {

    private final ProductRepository productRepository;
    private final ProductApiService productApiService;

    @GetMapping("/products/{productId}")
    ResponseEntity<ProductCommonDto> getProductById(@PathVariable("productId") Long productId){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductCommonDto productCommonDto =
                new ProductCommonDto(product.getId(), product.getTitle(), product.getPrice(), product.getStock());

        return ResponseEntity.ok().body(productCommonDto);
    }

    @PostMapping("/products/{productId}/decrease-stock")
    void decreaseStock(@PathVariable ("productId") Long productId,
                       @RequestParam ("quantity") Integer quantity){
        productApiService.decreaseStock(productId, quantity);
    }

    @PostMapping("/products/{productId}/increase-stock")
    void increaseStock(@PathVariable ("productId") Long productId,
                       @RequestParam ("quantity") Integer quantity){
        productApiService.increaseStock(productId, quantity);
    }
}
