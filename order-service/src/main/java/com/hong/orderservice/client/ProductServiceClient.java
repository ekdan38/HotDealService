package com.hong.orderservice.client;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.dto.ProductStockDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @PostMapping("/product-service/products")
    List<ProductCommonDto> getProductsById(@RequestBody List<ProductStockDto> productIds);

    @PostMapping("/product-service/products/decrease-stock")
    String decreaseStock(@RequestBody List<ProductStockDto> productStockDtos);

    @PostMapping("/product-service/products/{productId}/increase-stock")
    void increaseStock(@PathVariable ("productId") Long productId,
                       @RequestParam ("quantity") Integer quantity);
}
