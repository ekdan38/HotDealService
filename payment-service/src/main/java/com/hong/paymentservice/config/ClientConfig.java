package com.hong.paymentservice.config;

import feign.Logger;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {

    @Bean
    public Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }

    @Bean
    public Retryer neverRetry() {
        // spring-retry 비활성화 첫 번째 시도만 시도, 재시도 없음
        return new Retryer.Default(0, 0, 1);
    }
}
