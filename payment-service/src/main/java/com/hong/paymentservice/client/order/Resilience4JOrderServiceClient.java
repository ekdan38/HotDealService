package com.hong.paymentservice.client.order;

import com.hong.common.dto.OrderFetchRequestDto;
import com.hong.common.dto.OrderFetchResponseDto;
import com.hong.common.dto.OrderUpdateRequestDto;
import com.hong.common.exception.custom.PaymentException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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

    // Order, Delivery update
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerUpdateOrderStatus")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryUpdateOrderStatus")
    @PostMapping("/order-service/update")
    public Boolean updateOrderStatus(OrderUpdateRequestDto requestDto) {
        return orderServiceClient.updateOrderStatus(requestDto);
    }


    // order 조회 CircuitBreaker Fallback Method
    private OrderFetchResponseDto fallBackForCircuitBreakerFetchOrder(OrderFetchRequestDto requestDto, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("order-Service fetchOrder 호출 실패 userId = {}, orderId = {}, Error = {}",
                requestDto.getUserId(),
                requestDto.getOrderId(),
                throwable.getMessage());
        return new OrderFetchResponseDto();
    }

    // product 재고 감소 Retry Fallback Method
    private OrderFetchResponseDto fallbackForRetryFetchOrder(OrderFetchRequestDto requestDto, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("order-Service fetchOrder 최종 실패 userId = {}, orderId = {}, Error = {}",
                requestDto.getUserId(),
                requestDto.getOrderId(),
                throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

    // Order, Delivery update CircuitBreaker Fallback Method
    private Boolean fallBackForCircuitBreakerUpdateOrderStatus(OrderUpdateRequestDto requestDto, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("order-Service updateOrderStatus 호출 실패 userId = {}, orderId = {}, Error = {}",
                requestDto.getUserId(),
                requestDto.getOrderId(),
                throwable.getMessage());
        return false;
    }

    // Order, Delivery update Retry Fallback Method
    private Boolean fallbackForRetryUpdateOrderStatus(OrderUpdateRequestDto requestDto, Throwable throwable) {
        // PaymentException 이면 그대로 다시 예외 던진다. (globalExceptionHandler 에서 처리)
        if(throwable instanceof PaymentException) throw (PaymentException) throwable;
        log.error("order-Service updateOrderStatus 최종 실패 userId = {}, orderId = {}, Error = {}",
                requestDto.getUserId(),
                requestDto.getOrderId(),
                throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

}
