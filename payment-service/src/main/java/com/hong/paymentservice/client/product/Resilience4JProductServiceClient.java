package com.hong.paymentservice.client.product;

import com.hong.common.dto.OrderProductDto;
import com.hong.common.dto.ProductStockCheckRequestDto;
import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.common.exception.custom.OrderException;
import com.hong.common.exception.custom.PaymentException;
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

    // Product 재고 감소
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerDecreaseStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryDecreaseStock")
    public List<ProductStockUpdateResponseDto> decreaseStock(List<ProductStockUpdateRequestDto> requestDtos) {
        return productServiceClient.decreaseStock(requestDtos);
    }

    // Product 재고 증가
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerIncreaseStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryIncreaseStock")
    public List<ProductStockUpdateResponseDto> increaseStock(List<ProductStockUpdateRequestDto> requestDtos) {
        return productServiceClient.increaseStock(requestDtos);
    }

    // product 재고 감소 CircuitBreaker Fallback Method
    private List<ProductStockUpdateResponseDto> fallBackForCircuitBreakerDecreaseStock(List<ProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("product-service decreaseStock 호출 실패 requestDtos = {}, Error = {}", requestDtos, throwable.getMessage());
        return new ArrayList<>();
    }

    // product 재고 감소 Retry Fallback Method
    public List<ProductStockUpdateResponseDto> fallbackForRetryDecreaseStock(List<ProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("product-service decreaseStock 최종 실패: requestDtos = {}, Error = {}", requestDtos, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

    // product 재고 증가 CircuitBreaker Fallback Method
    private List<ProductStockUpdateResponseDto> fallBackForCircuitBreakerIncreaseStock(List<ProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("product-service increaseStock 호출 실패 requestDtos = {}, Error = {}", requestDtos, throwable.getMessage());
        return new ArrayList<>();
    }

    // product 재고 증가 Retry Fallback Method
    public List<ProductStockUpdateResponseDto> fallbackForRetryIncreaseStock(List<ProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("product-service increaseStock 최종 실패: requestDtos = {}, Error = {}", requestDtos, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }
}
