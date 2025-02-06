package com.hong.orderservice.client.product;

import com.hong.common.dto.ProductStockCheckRequestDto;
import com.hong.common.dto.ProductStockCheckResponseDto;
import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @PostMapping("/product-service/products")
    List<ProductStockCheckResponseDto> fetchProducts(@RequestBody List<ProductStockCheckRequestDto> requestDtos);

    @PostMapping("/product-service/decrease-stock")
    List<ProductStockUpdateResponseDto> decreaseStock (@RequestBody List<ProductStockUpdateRequestDto> requestDtos);

    @PostMapping("/product-service/increase-stock")
    List<ProductStockUpdateResponseDto> increaseStock (@RequestBody List<ProductStockUpdateRequestDto> requestDtos);
}
