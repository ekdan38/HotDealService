package com.hong.orderservice.client.product;

import com.hong.common.dto.ProductCommonDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @PostMapping("/product-service/products/increase-stock")
    List<ProductCommonDto> fetchAndIncreaseStock(@RequestBody List<ProductCommonDto> productCommonDtos);

    @PostMapping("/product-service/products/decrease-stock")
    List<ProductCommonDto> fetchAndDecreaseStock(@RequestBody List<ProductCommonDto> productCommonDtos);


}
