package com.hong.hotdealservice.client;

import com.hong.common.dto.ProductCommonDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/product-service/products")
    List<ProductCommonDto> getProductsById(@RequestParam List<Long> productIds);
}
