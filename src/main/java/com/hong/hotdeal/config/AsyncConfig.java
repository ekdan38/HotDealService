package com.hong.hotdeal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);  // 기본적으로 실행할 스레드 수
        executor.setMaxPoolSize(10); // 최대 실행 가능 스레드 수
        executor.setQueueCapacity(50); // Queue에 들어갈 작업 수
        executor.setThreadNamePrefix("EmailAsyncExecutor-");
        executor.initialize();
        return executor;
    }
}
