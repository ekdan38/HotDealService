package com.hong.orderservice.client.payment;

import com.hong.common.dto.PaymentCancelRequestDto;
import com.hong.common.dto.PaymentCancelResponseDto;
import com.hong.common.dto.PaymentCreateRequestDto;
import com.hong.common.dto.PaymentCreateResponseDto;
import com.hong.common.exception.custom.OrderException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Resilience4JPaymentServiceClient")
public class Resilience4JPaymentServiceClient {

    private final PaymentServiceClient paymentServiceClient;

    /**
     * paymentService payment 생성 요청
     */
    @CircuitBreaker(name = "custom", fallbackMethod = "fallBackForCircuitBreakerCreatePayment")
    @Retry(name = "custom", fallbackMethod = "fallbackForRetryCreatePayment")
    public PaymentCreateResponseDto createPayment(PaymentCreateRequestDto requestDto) {
        return paymentServiceClient.createPayment(requestDto);
    }

    // createPayment CircuitBreaker Fallback
    private PaymentCreateResponseDto fallBackForCircuitBreakerCreatePayment(PaymentCreateRequestDto requestDto, Throwable throwable) {
        log.error("[CircuitBreaker Fallback] payment-service 호출 실패. orderId={}, error={}", requestDto.getOrderId(), throwable.getMessage());
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        return new PaymentCreateResponseDto(true);
    }

    // createPayment Retry Fallback
    private PaymentCreateResponseDto fallbackForRetryCreatePayment(PaymentCreateRequestDto requestDto, Throwable throwable) {
        log.error("[RETRY Fallback] payment-service 호출 최종 재시도 실패. orderId={}, error={}", requestDto.getOrderId(), throwable.getMessage());
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        throw new RuntimeException();
    }

    /**
     * paymentService payment 취소 요청
     */
    @CircuitBreaker(name = "custom", fallbackMethod = "fallBackForCircuitBreakerCancelPayment")
    @Retry(name = "custom", fallbackMethod = "fallbackForRetryCancelPayment")
    public PaymentCancelResponseDto cancelPayment(PaymentCancelRequestDto requestDto) {
        return paymentServiceClient.cancelPayment(requestDto);
    }

    // cancelPayment CircuitBreaker Fallback
    private PaymentCancelResponseDto fallBackForCircuitBreakerCancelPayment(PaymentCancelRequestDto requestDto, Throwable throwable) {
        log.error("[CircuitBreaker Fallback] payment-service 호출 실패. orderId = {}, error = {}", requestDto.getOrderId(), throwable.getMessage());
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        return new PaymentCancelResponseDto(false, true);
    }

    // cancelPayment Retry Fallback
    private PaymentCancelResponseDto fallbackForRetryCancelPayment(PaymentCancelRequestDto requestDto, Throwable throwable) {
        log.error("[RETRY Fallback] payment-service 호출 최종 재시도 실패. orderId = {}, error = {}", requestDto.getOrderId(), throwable.getMessage());
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        throw new RuntimeException();
    }
}
