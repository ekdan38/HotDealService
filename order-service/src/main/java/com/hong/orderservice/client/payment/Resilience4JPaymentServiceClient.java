package com.hong.orderservice.client.payment;

import com.hong.common.dto.*;
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
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerCreatePayment")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryCreatePayment")
    public PaymentCreateResponseDto createPayment(PaymentCreateRequestDto requestDto) {
        return paymentServiceClient.createPayment(requestDto);
    }

    // createPayment CircuitBreaker Fallback
    private PaymentCreateResponseDto fallBackForCircuitBreakerCreatePayment(PaymentCreateRequestDto requestDto, Throwable t) {
        log.error("[CircuitBreaker OPEN] payment-service 호출 차단. orderId={}, error={}", requestDto.getOrderId(), t.getMessage());
        return new PaymentCreateResponseDto(true);
    }

    // createPayment Retry Fallback
    private PaymentCreateResponseDto fallbackForRetryCreatePayment(PaymentCreateRequestDto requestDto, Throwable t) {
        log.error("[RETRY FAIL] payment-service 호출 최종 재시도 실패. orderId={}, error={}", requestDto.getOrderId(), t.getMessage());
        return new PaymentCreateResponseDto(true);
    }

    /**
     * paymentService payment Expire 요청
     */
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerExpirePayment")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryExpirePayment")
    public ExpirePaymentResponseDto expirePayment(ExpirePaymentRequestDto requestDto) {
        return paymentServiceClient.expirePayment(requestDto);
    }

    // expirePayment CircuitBreaker Fallback
    private ExpirePaymentResponseDto fallBackForCircuitBreakerExpirePayment(ExpirePaymentRequestDto requestDto, Throwable t) {
        log.error("[CircuitBreaker OPEN] payment-service 호출 차단. orderIds = {}, error = {}", requestDto.getOrderId(), t.getMessage());
        return new ExpirePaymentResponseDto(false, true);
    }

    // expirePayment Retry Fallback
    private ExpirePaymentResponseDto fallbackForRetryExpirePayment(ExpirePaymentRequestDto requestDto, Throwable t) {
        log.error("[RETRY FAIL] payment-service 호출 최종 재시도 실패. orderIds = {}, error = {}", requestDto.getOrderId(), t.getMessage());
        return new ExpirePaymentResponseDto(false, true);
    }

    /**
     * paymentService payment 취소 요청
     */
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerCancelPayment")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryCancelPayment")
    public PaymentCancelResponseDto cancelPayment(PaymentCancelRequestDto requestDto) {
        return paymentServiceClient.cancelPayment(requestDto);
    }

    // cancelPayment CircuitBreaker Fallback
    private PaymentCancelResponseDto fallBackForCircuitBreakerCancelPayment(PaymentCancelRequestDto requestDto, Throwable t) {
        log.error("[CircuitBreaker OPEN] payment-service 호출 차단. orderId = {}, error = {}", requestDto.getOrderId(), t.getMessage());
        return new PaymentCancelResponseDto(false, true);
    }

    // cancelPayment Retry Fallback
    private PaymentCancelResponseDto fallbackForRetryCancelPayment(PaymentCancelRequestDto requestDto, Throwable t) {
        log.error("[RETRY FAIL] payment-service 호출 최종 재시도 실패. orderId = {}, error = {}", requestDto.getOrderId(), t.getMessage());
        return new PaymentCancelResponseDto(false, true);
    }
}
