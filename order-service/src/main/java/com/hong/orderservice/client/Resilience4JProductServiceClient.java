package com.hong.orderservice.client;

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


    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerIncreaseStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryIncreaseStock")
    public boolean increaseStock(List<ProductStockDto> stockIncreaseDto) {
        return productServiceClient.increaseStock(stockIncreaseDto);
    }
    private boolean fallBackForCircuitBreakerIncreaseStock(List<ProductStockDto> stockIncreaseDto, Throwable throwable){
        log.error("IncreaseStock FeignClient 호출 실패 stockIncreaseDto = {}, Error = {}", stockIncreaseDto, throwable.getMessage());
        return false;
    }
    public boolean fallbackForRetryIncreaseStock(List<ProductStockDto> stockIncreaseDto, Throwable throwable) {
        log.error("IncreaseStock Retry 최종 실패: stockIncreaseDto = {}, Error = {}", stockIncreaseDto, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }


    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerIncreaseStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryIncreaseStock")
    public boolean decreaseStock(List<ProductStockDto> stockDecreaseDto) {
        return productServiceClient.decreaseStock(stockDecreaseDto);
    }
    private boolean fallBackForCircuitBreakerDecreaseStock(List<ProductStockDto> stockDecreaseDto, Throwable throwable){
        log.error("DecreaseStock FeignClient 호출 실패 stockDecreaseDto = {}, Error = {}", stockDecreaseDto, throwable.getMessage());
        return false;
    }
    public boolean fallbackForRetryDecreaseStock(List<ProductStockDto> stockDecreaseDto, Throwable throwable) {
        log.error("DecreaseStock Retry 최종 실패: stockDecreaseDto = {}, Error = {}", stockDecreaseDto, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

}
