package com.hong.orderservice.client.hotdeal;

import com.hong.common.dto.HotDealProductCommonDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Resilience4JHotDealServiceClient")
public class Resilience4JHotDealServiceClient {

    private final HotDealServiceClient hotDealServiceClient;


    // HotDealProduct 재고 감소
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerDecreaseHotDealProducts")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryDecreaseHotDealProducts")
    public List<HotDealProductCommonDto> fetchAndDecreaseStock(List<HotDealProductCommonDto> hotDealProductCommonDtos) {
        return hotDealServiceClient.fetchAndDecreaseStock(hotDealProductCommonDtos);
    }

    // HotDealProduct 재고 증가
    @CircuitBreaker(name = "default", fallbackMethod = "fallBackForCircuitBreakerIncreaseHotDealProducts")
    @Retry(name = "default", fallbackMethod = "fallbackForRetryIncreaseHotDealProducts")
    public List<HotDealProductCommonDto> fetchAndIncreaseStock(List<HotDealProductCommonDto> hotDealProductCommonDtos) {
        return hotDealServiceClient.fetchAndIncreaseStock(hotDealProductCommonDtos);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // HotDealProduct 재고 감소 CircuitBreaker Fallback
    private List<HotDealProductCommonDto> fallBackForCircuitBreakerDecreaseHotDealProducts(List<HotDealProductCommonDto> hotDealProductCommonDtos, Throwable throwable) {
        log.error("fetchAndDecreaseStock 호출 실패 hotDealProductIds = {}, Error = {}", extractHotDealProductIds(hotDealProductCommonDtos), throwable.getMessage());
        return new ArrayList<>();
    }

    // HotDealProduct 재고 감소 Retry Fallback
    public List<HotDealProductCommonDto> fallbackForRetryDecreaseHotDealProducts(List<HotDealProductCommonDto> hotDealProductCommonDtos, Throwable throwable) {
        log.error("fetchAndDecreaseStock 최종 실패: hotDealProducts = {}, Error = {}", extractHotDealProductIds(hotDealProductCommonDtos), throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

    // HotDealProduct 재고 증가 CircuitBreaker Fallback
    private List<HotDealProductCommonDto> fallBackForCircuitBreakerIncreaseHotDealProducts(List<HotDealProductCommonDto> hotDealProductCommonDtos, Throwable throwable) {
        log.error("fetchAndDecreaseStock 호출 실패 hotDealProductIds = {}, Error = {}", extractHotDealProductIds(hotDealProductCommonDtos), throwable.getMessage());
        return new ArrayList<>();
    }

    // HotDealProduct 재고 증가 Retry Fallback
    public List<HotDealProductCommonDto> fallbackForRetryIncreaseHotDealProducts(List<HotDealProductCommonDto> hotDealProductCommonDtos, Throwable throwable) {
        log.error("fetchAndDecreaseStock 최종 실패: hotDealProducts = {}, Error = {}", extractHotDealProductIds(hotDealProductCommonDtos), throwable.getMessage());
        throw new RuntimeException("Retry 최종 실패");
    }

    private List<Long> extractHotDealProductIds(List<HotDealProductCommonDto> hotDealProductCommonDtos){
        return hotDealProductCommonDtos.stream().map(HotDealProductCommonDto::getHotDealProductId).collect(Collectors.toList());
    }
}
