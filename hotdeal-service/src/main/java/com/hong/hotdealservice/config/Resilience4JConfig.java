package com.hong.hotdealservice.config;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.spring6.retry.configure.RetryConfigurationProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
@Slf4j
public class Resilience4JConfig {
    private static final int PRIORITY_1 = -3; // Retry의 우선순위
    private static final int PRIORITY_2 = -4; // CircuitBreaker의 우선순위

    private final CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties;
    private final RetryConfigurationProperties retryConfigurationProperties;

    public Resilience4JConfig(CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties,
                              RetryConfigurationProperties retryConfigurationProperties) {
        this.circuitBreakerConfigurationProperties = circuitBreakerConfigurationProperties;
        this.retryConfigurationProperties = retryConfigurationProperties;
    }
    @PostConstruct
    public void setOrder() {
        circuitBreakerConfigurationProperties.setCircuitBreakerAspectOrder(PRIORITY_2);
        retryConfigurationProperties.setRetryAspectOrder(PRIORITY_1);
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(){
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                //  50% 실패 시 OPEN
                .failureRateThreshold(10)
                // OPEN 상태에서 10초 후 HALF_OPEN 으로 전환
                .waitDurationInOpenState(Duration.ofSeconds(10))
                // 카운트 기반의 슬라이딩 윈도우, 20회 호출 기록으로 실패율 계산
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                // 최소 10건의 호출이 쌓여야 실패율 계산
                .minimumNumberOfCalls(10)
                // HALF_OPEN 상태에서 3건 연속 성공이면 CLOSED 로 전환
                .permittedNumberOfCallsInHalfOpenState(3)
                // 2초 이상 걸린 호출을 느린 호출 로 간주
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                // 느린 호출이 50% 이상이면 실패로 간주
                .slowCallRateThreshold(50)
                .build();
        return CircuitBreakerRegistry.of(circuitBreakerConfig);

    }

    @Bean
    public RetryRegistry retryRegistry() {
        // Retry 설정
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3) // 최대 3번 재시도
                .waitDuration(Duration.ofMillis(500)) // 각 재시도 간격
                .retryExceptions(IOException.class, TimeoutException.class, FeignException.class) // 재시도 대상 예외
                .build();

        // RetryRegistry 생성
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);

        // 이벤트 리스너 등록 (Retry 인스턴스마다 이벤트 발생 시 호출)
        retryRegistry.retry("default").getEventPublisher()
                .onRetry(event -> log.warn("[Retry] 재시도 발생: {}, Attempt: {}, Last Throwable: {}",
                        event.getName(),
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "N/A"))
                .onSuccess(event -> log.info("[Retry] 성공: {}, Attempt: {}",
                        event.getName(),
                        event.getNumberOfRetryAttempts()))
                .onError(event -> log.error("[Retry] 실패: {}, Error: {}",
                        event.getName(),
                        event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "N/A"))
                .onIgnoredError(event -> log.info("[Retry] 무시된 오류: {}, Error: {}",
                        event.getName(),
                        event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "N/A"));
        return retryRegistry;

    }

}

