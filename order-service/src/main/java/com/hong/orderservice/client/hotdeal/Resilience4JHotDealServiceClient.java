package com.hong.orderservice.client.hotdeal;

import com.hong.common.dto.*;
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
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerReserveStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryReserveStock")
    public ProductReservationResponseDto reserveStock(ProductReservationRequestDto requestDto) {
        return hotDealServiceClient.reserveStock(requestDto);
    }

    // stock Reserve CircuitBreaker Fallback
    private ProductReservationResponseDto fallBackForCircuitBreakerReserveStock(ProductReservationRequestDto requestDto, Throwable throwable) {
        log.error("[CircuitBreaker Fallback] hotDeal-service 호출 실패. orderId={}, error={}", requestDto.getOrderId(), throwable.getMessage());
        return new ProductReservationResponseDto(true);
    }

    // stock Reserve Retry Fallback
    private ProductReservationResponseDto fallbackForRetryReserveStock(ProductReservationRequestDto requestDto, Throwable throwable) {
        log.error("[RETRY Fallback] hotDeal-service 호출 최종 재시도 실패. orderId={}, error={}", requestDto.getOrderId(), throwable.getMessage());
        return new ProductReservationResponseDto(true);
    }

    /**
     *  hotDealService 재고 최종 반영 요청
     */
    // HotDealProduct stock 최종 반영
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerFinalizeStockReservation")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryFinalizeStockReservation")
    public StockFinalizeResponseDto finalizeStockReservation (StockFinalizeRequestDto requestDto) {
        return hotDealServiceClient.finalizeStockReservation(requestDto);
    }

    // stock 최종 반영 CircuitBreaker Fallback
    private StockFinalizeResponseDto fallBackForCircuitBreakerFinalizeStockReservation(StockFinalizeRequestDto requestDto, Throwable throwable) {
        log.error("[CircuitBreaker OPEN] hotDeal-service 호출 차단. orderId={}, error={}", requestDto.getOrderId(), throwable.getMessage());
        return new StockFinalizeResponseDto(false, true);
    }

    // stock 최종 반영 Retry Fallback
    private StockFinalizeResponseDto fallbackForRetryFinalizeStockReservation(StockFinalizeRequestDto requestDto, Throwable throwable) {
        log.error("[RETRY FAIL] hotDeal-service 호출 최종 재시도 실패. orderId={}, error={}", requestDto.getOrderId(), throwable.getMessage());
        return new StockFinalizeResponseDto(false, true);
    }

    /**
     * hotDealService 재고 점유 테이블 삭제 요청
     */
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerReleaseReservedStocks")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryReleaseReservedStocks")
    public ReleaseReservedStockResponseDto releaseReservedStocks(ReleaseReservedStockRequestDto requestDto) {
        return hotDealServiceClient.releaseReservedStocks(requestDto);
    }

    // reservedStock 삭제 CircuitBreaker Fallback
    private ReleaseReservedStockResponseDto fallBackForCircuitBreakerReleaseReservedStocks(ReleaseReservedStockRequestDto requestDto, Throwable t) {
        log.error("[CircuitBreaker OPEN] hotDeal-service 호출 차단. orderIds={}, error={}", requestDto.getOrderId(), t.getMessage());
        return new ReleaseReservedStockResponseDto(false, true);
    }

    // reservedStock 삭제 Retry Fallback
    private ReleaseReservedStockResponseDto fallbackForRetryReleaseReservedStocks(ReleaseReservedStockRequestDto requestDto, Throwable t) {
        log.error("[RETRY FAIL] hotDeal-service 호출 최종 재시도 실패. orderId={}, error={}", requestDto.getOrderId(), t.getMessage());
        return new ReleaseReservedStockResponseDto(false, true);
    }

    /**
     *  hotDealService 재고 복구 요청
     *  주문 취소
     */
    // HotDealProduct stock 복구 요청
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerRestoreStock")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryRestoreStock")
    public StockRestoreResponseDto restoreStock (StockRestoreRequestDto requestDto) {
        return hotDealServiceClient.restoreStock(requestDto);
    }

    // restoreStock CircuitBreaker Fallback
    private StockRestoreResponseDto fallBackForCircuitBreakerRestoreStock(StockRestoreRequestDto requestDto, Throwable t) {
        log.error("[CircuitBreaker OPEN] hotDeal-service 호출 차단. orderId={}, error={}", requestDto.getOrderId(), t.getMessage());
        return new StockRestoreResponseDto(false, true);
    }

    // restoreStock Retry Fallback
    private StockRestoreResponseDto fallbackForRetryRestoreStock(StockRestoreRequestDto requestDto, Throwable t) {
        log.error("[RETRY FAIL] hotDeal-service 호출 최종 재시도 실패. orderId={}, error={}", requestDto.getOrderId(), t.getMessage());
        return new StockRestoreResponseDto(false, true);
    }
}
