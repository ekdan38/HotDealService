package com.hong.hotdealservice.client;

import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @PostMapping("/product-service/decrease-stock")
    List<ProductStockUpdateResponseDto> decreaseStock (@RequestBody List<ProductStockUpdateRequestDto> requestDtos);

    @PostMapping("/product-service/increase-stock")
    List<ProductStockUpdateResponseDto> increaseStock (@RequestBody List<ProductStockUpdateRequestDto> requestDtos);
}
