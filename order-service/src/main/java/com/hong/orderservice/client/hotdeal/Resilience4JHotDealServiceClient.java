package com.hong.orderservice.client.hotdeal;

import com.hong.common.dto.*;
import com.hong.common.exception.custom.OrderException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Resilience4JHotDealServiceClient")
public class Resilience4JHotDealServiceClient {

    private final HotDealServiceClient hotDealServiceClient;

    // HotDealProduct 조회
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerFetchProducts")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryFetchProducts")
    public List<HotDealProductStockCheckResponseDto> fetchProductsAndValidateStock(List<HotDealProductStockCheckRequestDto> requestDtos) {
        return hotDealServiceClient.fetchProductsAndValidateStock(requestDtos);
    }

    // HotDealProduct 재고 감소
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerDecreaseStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryDecreaseStock")
    public List<HotDealProductStockUpdateResponseDto> decreaseStock(@RequestBody List<HotDealProductStockUpdateRequestDto> requestDtos){
        return hotDealServiceClient.decreaseStock(requestDtos);
    }

    // HotDealProduct 재고 증가
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerIncreaseStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryIncreaseStock")
    public List<HotDealProductStockUpdateResponseDto> increaseStock(@RequestBody List<HotDealProductStockUpdateRequestDto> requestDtos){
        return hotDealServiceClient.increaseStock(requestDtos);
    }


    // HotDealProduct 상품, 재고 조회 CircuitBreaker Fallback Method
    private List<HotDealProductStockCheckResponseDto> fallBackForCircuitBreakerFetchProducts(List<HotDealProductStockCheckRequestDto> requestDtos, Throwable throwable) {
        // OrderException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        log.error("hotDeal-service fetchProducts 호출 실패 requestDtos = {}, Error = {}", requestDtos, throwable.getMessage());
        return new ArrayList<>();
    }

    // HotDealProduct 상품, 재고 조회 Retry Fallback Method
    private List<HotDealProductStockCheckResponseDto> fallbackForRetryFetchProducts(List<HotDealProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // OrderException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        log.error("hotDeal-service fetchProducts 최종 실패: requestDtos = {}, Error = {}", requestDtos, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

    // HotDealProduct 재고 감소 CircuitBreaker Fallback Method
    private List<HotDealProductStockUpdateResponseDto> fallBackForCircuitBreakerDecreaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // OrderException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        log.error("hotDeal-service decreaseStock 호출 실패 requestDtos = {}, Error = {}", requestDtos, throwable.getMessage());
        return new ArrayList<>();
    }

    // HotDealProduct 재고 감소  Retry Fallback Method
    private List<HotDealProductStockUpdateResponseDto> fallbackForRetryDecreaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // OrderException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        log.error("hotDeal-service decreaseStock 최종 실패: requestDtos = {}, Error = {}", requestDtos, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

    // HotDealProduct 상품, 증가 조회 CircuitBreaker Fallback Method
    private List<HotDealProductStockUpdateResponseDto> fallBackForCircuitBreakerIncreaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // OrderException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        log.error("hotDeal-service increaseStock 호출 실패 requestDtos = {}, Error = {}", requestDtos, throwable.getMessage());
        return new ArrayList<>();
    }

    // HotDealProduct 상품, 증가 조회 Retry Fallback Method
    private List<HotDealProductStockUpdateResponseDto> fallbackForRetryIncreaseStock(List<HotDealProductStockCheckRequestDto> requestDtos, Throwable throwable) {
        // OrderException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        log.error("hotDeal-service increase 최종 실패: requestDtos = {}, Error = {}", requestDtos, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

}
