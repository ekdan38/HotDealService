package com.hong.productservice.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final Environment env;
    private String host;
    private int port;

    @PostConstruct
    public void init(){
        this.host = env.getProperty("spring.data.redis.host");
        this.port = Integer.parseInt(env.getProperty("spring.data.redis.port"));
    }

    // Lettuce 라이브러리 활용해 redis 연결을 관리 하는 객체 생성, redis 서버에 대한 정보(host, port) 설정
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(){
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
    }
}
