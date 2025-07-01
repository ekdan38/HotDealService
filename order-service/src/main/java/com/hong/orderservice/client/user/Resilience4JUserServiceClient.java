package com.hong.orderservice.client.user;

import com.hong.common.dto.UserCartDeleteRequestDto;
import com.hong.common.dto.UserCartDeleteResponseDto;
import com.hong.common.exception.custom.OrderException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Resilience4JUserServiceClient")
public class Resilience4JUserServiceClient {

    private final UserServiceClient userServiceClient;

    /**
     * userService userCart 삭제 요청
     */
    @CircuitBreaker(name = "default", fallbackMethod = "fallbackForCircuitBreaker")
    @Retry(name = "default", fallbackMethod = "fallbackForRetry")
    public UserCartDeleteResponseDto deleteUserCart(UserCartDeleteRequestDto requestDto) {
        return userServiceClient.deleteUserCart(requestDto);
    }

    // CircuitBreaker Fallback
    private UserCartDeleteResponseDto fallbackForCircuitBreaker(UserCartDeleteRequestDto requestDto, Throwable t) {
        log.error("[CircuitBreaker Fallback] user-service 호출 실패. userId = {}, error = {}", requestDto.getUserId(), t.getMessage());
        if(t instanceof OrderException ex) throw ex;
        return new UserCartDeleteResponseDto(false, true);
    }

    // Retry Fallback
    private UserCartDeleteResponseDto fallbackForRetry(UserCartDeleteRequestDto requestDto, Throwable t) {
        log.error("[RETRY Fallback] user-service 호출 최종 재시도 실패. userId = {}, error = {}", requestDto.getUserId(), t.getMessage());
        if(t instanceof OrderException ex) throw ex;
        return new UserCartDeleteResponseDto(false, true);
    }

}
