package com.hong.hotdealservice.client;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.dto.ProductStockDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Resilience4JProductServiceClient")
public class Resilience4JProductServiceClient {

    private final ProductServiceClient productServiceClient;

    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerGetProductsByIds")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryGetProductsByIds")
    public List<ProductCommonDto> getProductsByIds(List<Long> productIds){
        return productServiceClient.getProductsById(productIds);
    }
    private List<ProductCommonDto> fallBackForCircuitBreakerGetProductsByIds(List<Long> productIds, Throwable throwable){
        log.error("getProductsByIds FeignClient 호출 실패 productIds = {}, Error = {}", productIds, throwable.getMessage());
        return new ArrayList<>();
    }
    public List<ProductCommonDto> fallbackForRetryGetProductsByIds(List<Long> productIds, Throwable throwable) {
        log.error("GetProductsByIds Retry 최종 실패: productIds = {}, Error = {}", productIds, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }
}
