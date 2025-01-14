package com.hong.orderservice.client;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.dto.ProductStockDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/product-service/products")
    List<ProductCommonDto> getProductsById(@RequestBody List<Long> productIds);

    @PostMapping("/product-service/products/decrease-stock")
    Boolean decreaseStock(@RequestBody List<ProductStockDto> productStockDtos);

    @PostMapping("/product-service/products/increase-stock")
    Boolean increaseStock(@RequestBody List<ProductStockDto> productStockDtos);
}
