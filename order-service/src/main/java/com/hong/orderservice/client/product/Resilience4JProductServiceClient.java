package com.hong.orderservice.client.product;

import com.hong.common.dto.HotDealProductCommonDto;
import com.hong.common.dto.ProductCommonDto;
import com.hong.common.exception.custom.OrderException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Resilience4JProductServiceClient")
public class Resilience4JProductServiceClient {

    private final ProductServiceClient productServiceClient;

    // Product 재고 감소
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerDecreaseProducts")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryDecreaseProducts")
    public List<ProductCommonDto> fetchAndDecreaseStock(List<ProductCommonDto> productCommonDtos) {
        return productServiceClient.fetchAndDecreaseStock(productCommonDtos);
    }

    // Product 재고 증가
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerProducts")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryIncreaseProducts")
    public List<ProductCommonDto> fetchAndIncreaseStock(List<ProductCommonDto> productCommonDtos) {
        return productServiceClient.fetchAndIncreaseStock(productCommonDtos);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // product 재고 감소 CircuitBreaker Fallback
    private List<HotDealProductCommonDto> fallBackForCircuitBreakerDecreaseProducts(List<ProductCommonDto> productCommonDtos, Throwable throwable) {
        // OrderException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        log.error("fetchAndDecreaseStock 호출 실패 hotDealProductIds = {}, Error = {}", extractHotDealProductIds(productCommonDtos), throwable.getMessage());
        return new ArrayList<>();
    }

    // product 재고 감소 Retry Fallback
    public List<HotDealProductCommonDto> fallbackForRetryDecreaseProducts(List<ProductCommonDto> productCommonDtos, Throwable throwable) {
        // OrderException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        log.error("fetchAndDecreaseStock 최종 실패: hotDealProducts = {}, Error = {}", extractHotDealProductIds(productCommonDtos), throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

    // product 재고 증가 CircuitBreaker Fallback
    private List<HotDealProductCommonDto> fallBackForCircuitBreakerProducts(List<ProductCommonDto> productCommonDtos, Throwable throwable) {
        // OrderException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        log.error("fetchAndDecreaseStock 호출 실패 hotDealProductIds = {}, Error = {}", extractHotDealProductIds(productCommonDtos), throwable.getMessage());
        return new ArrayList<>();
    }

    // product 재고 증가 Retry Fallback
    public List<HotDealProductCommonDto> fallbackForRetryIncreaseProducts(List<ProductCommonDto> productCommonDtos, Throwable throwable) {
        // OrderException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        log.error("fetchAndDecreaseStock 최종 실패: hotDealProducts = {}, Error = {}", extractHotDealProductIds(productCommonDtos), throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

    private List<Long> extractHotDealProductIds(List<ProductCommonDto> productCommonDtos){
        return productCommonDtos.stream().map(ProductCommonDto::getId).collect(Collectors.toList());
    }
}
