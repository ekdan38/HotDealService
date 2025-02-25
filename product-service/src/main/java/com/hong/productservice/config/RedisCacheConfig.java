package com.hong.productservice.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager productCacheManager(RedisConnectionFactory redisConnectionFactory){
        RedisCacheConfiguration getProductsConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Redis 에 key 저장할 때 String 으로 저장
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Redis 에 value 저장할 때 Json 으로 저장
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<Object>(Object.class)))
                // TTL 설정
                .entryTtl(Duration.ofMinutes(5L));

        RedisCacheConfiguration getProductConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Redis 에 key 저장할 때 String 으로 저장
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Redis 에 value 저장할 때 Json 으로 저장
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<Object>(Object.class)))
                // TTL 설정
                .entryTtl(Duration.ofMinutes(10L));


        // 캐시 이름별로 설정 구성
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("getProducts", getProductsConfig);
        cacheConfigurations.put("getProduct", getProductConfig);

        return RedisCacheManager.builder(redisConnectionFactory)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

}
