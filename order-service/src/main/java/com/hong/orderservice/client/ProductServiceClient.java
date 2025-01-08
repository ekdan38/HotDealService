package com.hong.orderservice.client;

import com.hong.common.dto.ProductCommonDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/product-service/products/{productId}")
    ProductCommonDto getProductById(@PathVariable ("productId") Long productId);

    @PostMapping("/product-service/products/{productId}/decrease-stock")
    void decreaseStock(@PathVariable ("productId") Long productId,
                       @RequestParam ("quantity") Integer quantity);

    @PostMapping("/product-service/products/{productId}/increase-stock")
    void increaseStock(@PathVariable ("productId") Long productId,
                       @RequestParam ("quantity") Integer quantity);
}
