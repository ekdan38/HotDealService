package com.hong.paymentservice.client;

import com.hong.common.dto.OrderFetchRequestDto;
import com.hong.common.dto.OrderFetchResponseDto;
import com.hong.common.dto.OrderUpdateRequestDto;
import com.hong.common.dto.OrderUpdateResponseDto;
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
    private OrderFetchResponseDto fallBackForCircuitBreakerFetchOrder(OrderFetchRequestDto requestDto, Throwable t) {
        log.error("[CircuitBreaker Open] order-service 호출 실패. orderId={}, error={}", requestDto.getOrderId(), t.getMessage());
        return new OrderFetchResponseDto(true);
    }

    // order 조회 Retry Fallback Method
    private OrderFetchResponseDto fallbackForRetryFetchOrder(OrderFetchRequestDto requestDto, Throwable t) {
        log.error("[RETRY Fallback] 최종 재시도 실패. orderId={}, error={}", requestDto.getOrderId(), t.getMessage());
        return new OrderFetchResponseDto(true);
    }


    // Order, Delivery update
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerUpdateOrderStatus")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryUpdateOrderStatus")
    public OrderUpdateResponseDto updateOrderStatus(OrderUpdateRequestDto requestDto) {
        return orderServiceClient.updateOrderStatus(requestDto);
    }

    // Order, Delivery update CircuitBreaker Fallback Method
    private OrderUpdateResponseDto fallBackForCircuitBreakerUpdateOrderStatus(OrderUpdateRequestDto requestDto, Throwable t) {
        log.error("[CircuitBreaker Open] order-service 호출 실패. orderId={}, error={}", requestDto.getOrderId(), t.getMessage());
        return new OrderUpdateResponseDto(false, true);
    }

    // Order, Delivery update Retry Fallback Method
    private OrderUpdateResponseDto fallbackForRetryUpdateOrderStatus(OrderUpdateRequestDto requestDto, Throwable t) {
        log.error("[RETRY Fallback] 최종 재시도 실패. orderId={}, error={}", requestDto.getOrderId(), t.getMessage());
        return new OrderUpdateResponseDto(false, true);
    }
}
