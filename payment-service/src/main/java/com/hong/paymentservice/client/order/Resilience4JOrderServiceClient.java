package com.hong.paymentservice.client.order;

import com.hong.common.dto.OrderFetchRequestDto;
import com.hong.common.dto.OrderFetchResponseDto;
import com.hong.common.exception.custom.PaymentException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Resilience4JOrderServiceClient")
public class Resilience4JOrderServiceClient {

    private final OrderServiceClient orderServiceClient;

    // Order 조회
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerFetchOrder")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryFetchOrder")
    public OrderFetchResponseDto fetchOrder(OrderFetchRequestDto requestDto) {
        return orderServiceClient.fetchOrder(requestDto);
    }

    // order 조회 CircuitBreaker Fallback Method
    private OrderFetchResponseDto fallBackForCircuitBreakerFetchOrder(OrderFetchRequestDto requestDto, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("fetchOrder 호출 실패 userId = {}, orderId = {}, Error = {}",
                requestDto.getUserId(),
                requestDto.getOrderId(),
                throwable.getMessage());
        return new OrderFetchResponseDto();
    }

    // product 재고 감소 Retry Fallback Method
    private OrderFetchResponseDto fallbackForRetryFetchOrder(OrderFetchRequestDto requestDto, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("fetchOrder 최종 실패 userId = {}, orderId = {}, Error = {}",
                requestDto.getUserId(),
                requestDto.getOrderId(),
                throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }


}
