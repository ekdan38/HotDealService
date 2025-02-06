package com.hong.paymentservice.client.hotDeal;

import com.hong.common.dto.HotDealProductStockUpdateRequestDto;
import com.hong.common.dto.HotDealProductStockUpdateResponseDto;
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
@Slf4j(topic = "Resilience4JHotDealServiceClient")
public class Resilience4JHotDealServiceClient {

    private final HotDealServiceClient hotDealServiceClient;

    // HotDealProduct 재고 감소
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerDecreaseStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryDecreaseStock")
    public List<HotDealProductStockUpdateResponseDto> decreaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos){
        return hotDealServiceClient.decreaseStock(requestDtos);
    }

    // HotDealProduct 재고 증가
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerIncreaseStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryIncreaseStock")
    public List<HotDealProductStockUpdateResponseDto> increaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos){
        return hotDealServiceClient.increaseStock(requestDtos);
    }

    // HotDealProduct 재고 감소 CircuitBreaker Fallback Method
    private List<HotDealProductStockUpdateResponseDto> fallBackForCircuitBreakerDecreaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("hotDeal-service decreaseStock 호출 실패 requestDtos = {}, Error = {}",requestDtos, throwable.getMessage());
        return new ArrayList<>();
    }

    // HotDealProduct 재고 감소 Retry Fallback
    private List<HotDealProductStockUpdateResponseDto> fallbackForRetryDecreaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("hotDeal-service decreaseStock 호출 최종 실패: requestDtos = {}, Error = {}", requestDtos, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

    // HotDealProduct 재고 증가 CircuitBreaker Fallback
    private List<HotDealProductStockUpdateResponseDto> fallBackForCircuitBreakerIncreaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("hotDeal-service increaseStock 호출 실패 hotDealProductIds = {}, Error = {}", requestDtos, throwable.getMessage());
        return new ArrayList<>();
    }

    // HotDealProduct 재고 증가 Retry Fallback
    private List<HotDealProductStockUpdateResponseDto> fallbackForRetryIncreaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("hotDeal-service increaseStock 호출 최종 실패: hotDealProducts = {}, Error = {}", requestDtos, throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

}
