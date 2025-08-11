package com.hong.orderservice.client.hotdeal;

import com.hong.common.dto.ProductReservationRequestDto;
import com.hong.common.dto.ProductReservationResponseDto;
import com.hong.common.dto.StockRestoreRequestDto;
import com.hong.common.dto.StockRestoreResponseDto;
import com.hong.common.exception.custom.OrderException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Resilience4JHotDealServiceClient")
public class Resilience4JHotDealServiceClient {

    private final HotDealServiceClient hotDealServiceClient;

    /**
     * hotDealService 재고 점유 요청
     */
    @Retry(name = "custom", fallbackMethod = "fallbackForRetryReserveStock")
    @CircuitBreaker(name = "custom", fallbackMethod = "fallBackForCircuitBreakerReserveStock")
    public ProductReservationResponseDto reserveStock(ProductReservationRequestDto requestDto) {
        log.info("===[재고 점유 요청 실행]===");
        return hotDealServiceClient.reserveStock(requestDto);
    }

    // stock Reserve CircuitBreaker Fallback
    private ProductReservationResponseDto fallBackForCircuitBreakerReserveStock(ProductReservationRequestDto requestDto, Throwable throwable) {
        log.error("[CircuitBreaker Fallback] hotDeal-service 호출 실패. orderId={}, error={}", requestDto.getOrderId(), throwable.getMessage());
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        return new ProductReservationResponseDto(true);
    }

    // stock Reserve Retry Fallback
    private ProductReservationResponseDto fallbackForRetryReserveStock(ProductReservationRequestDto requestDto, Throwable throwable) {
        log.error("[RETRY Fallback] hotDeal-service 호출 최종 재시도 실패. orderId={}, error={}", requestDto.getOrderId(), throwable.getMessage());
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        throw new RuntimeException();
    }


    /**
     *  hotDealService 재고 복구 요청 체크
     *  주문 취소
     */
    // HotDealProduct stock 복구 요청
    @CircuitBreaker(name = "custom", fallbackMethod = "fallBackForCircuitBreakerRestoreStock")
    @Retry(name = "custom", fallbackMethod = "fallbackForRetryRestoreStock")
    public StockRestoreResponseDto restoreStock (StockRestoreRequestDto requestDto) {
        return hotDealServiceClient.restoreStock(requestDto);
    }

    // restoreStock CircuitBreaker Fallback
    private StockRestoreResponseDto fallBackForCircuitBreakerRestoreStock(StockRestoreRequestDto requestDto, Throwable throwable) {
        log.error("[CircuitBreaker Fallback] hotDeal-service 호출 실패. orderId={}, error={}", requestDto.getOrderId(), throwable.getMessage());
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        return new StockRestoreResponseDto(false, true);
    }

    // restoreStock Retry Fallback
    private StockRestoreResponseDto fallbackForRetryRestoreStock(StockRestoreRequestDto requestDto, Throwable throwable) {
        log.error("[RETRY Fallback] hotDeal-service 호출 최종 재시도 실패. orderId={}, error={}", requestDto.getOrderId(), throwable.getMessage());
        if(throwable instanceof OrderException) throw (OrderException) throwable;
        throw new RuntimeException();
    }
}
