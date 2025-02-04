package com.hong.hotdealservice.client;

import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.common.exception.custom.HotDealProductException;
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

    // 원본 상품 재고 감소
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerDecreaseStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryDecreaseStock")
    public List<ProductStockUpdateResponseDto> decreaseStock(List<ProductStockUpdateRequestDto> requestDtos){
        return productServiceClient.decreaseStock(requestDtos);
    }

    // 원본 상품 재고 증가
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerIncreaseStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryIncreaseStock")
    public List<ProductStockUpdateResponseDto> increaseStock(List<ProductStockUpdateRequestDto> requestDtos){
        return productServiceClient.increaseStock(requestDtos);
    }

    // 원본 상품 재고 감소 circuitBreaker FallBack Method
    private List<ProductStockUpdateResponseDto> fallBackForCircuitBreakerDecreaseStock(List<ProductStockUpdateRequestDto> requestDtos, Throwable throwable){
        // HotDealProductException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof HotDealProductException) throw (HotDealProductException) throwable;
        log.error("product-service DecreaseStock FeignClient 호출 실패 request = {}, Error = {}", requestDtos, throwable.getMessage());
        return new ArrayList<>();
    }

    // 원본 상품 재고 감소 Retry FallBack Method
    private List<ProductStockUpdateResponseDto> fallbackForRetryDecreaseStock(List<ProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // HotDealProductException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof HotDealProductException) throw (HotDealProductException) throwable;
        log.error("product-service DecreaseStock FeignClient Retry 최종 실패: request = {}, Error = {}", requestDtos, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

    // 원본 상품 재고 증가 Retry FallBack Method
    private List<ProductStockUpdateResponseDto> fallBackForCircuitBreakerIncreaseStock(List<ProductStockUpdateRequestDto> requestDtos, Throwable throwable){
        // HotDealProductException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof HotDealProductException) throw (HotDealProductException) throwable;
        log.error("product-service IncreaseStock FeignClient 호출 실패 request = {}, Error = {}", requestDtos, throwable.getMessage());
        return new ArrayList<>();
    }

    // 원본 상품 재고 증가 Retry FallBack Method
    private List<ProductStockUpdateResponseDto> fallbackForRetryIncreaseStock(List<ProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // HotDealProductException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof HotDealProductException) throw (HotDealProductException) throwable;
        log.error("product-service IncreaseStock FeignClient Retry 최종 실패: request = {}, Error = {}", requestDtos, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }
}
