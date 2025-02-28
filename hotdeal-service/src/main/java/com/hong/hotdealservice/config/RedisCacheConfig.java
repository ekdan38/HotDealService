package com.hong.hotdealservice.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager HotDealCacheManager(RedisConnectionFactory redisConnectionFactory){
        ObjectMapper objectMapper = new ObjectMapper();
        // Java 8 날짜 직렬화 지원
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration getHotDealsConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Redis 에 key 저장할 때 String 으로 저장
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Redis 에 value 저장할 때 Json 으로 저장
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                // TTL 설정
                .entryTtl(Duration.ofMinutes(5L));

        RedisCacheConfiguration getHotDealConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Redis 에 key 저장할 때 String 으로 저장
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Redis 에 value 저장할 때 Json 으로 저장
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                // TTL 설정
                .entryTtl(Duration.ofMinutes(5L));

        RedisCacheConfiguration getHotDealProductsConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Redis 에 key 저장할 때 String 으로 저장
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Redis 에 value 저장할 때 Json 으로 저장
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                // TTL 설정
                .entryTtl(Duration.ofMinutes(5L));

        RedisCacheConfiguration getHotDealProductConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Redis 에 key 저장할 때 String 으로 저장
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Redis 에 value 저장할 때 Json 으로 저장
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                // TTL 설정
                .entryTtl(Duration.ofMinutes(5L));


        // 캐시 이름별로 설정 구성
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("getHotDeals", getHotDealsConfig);
        cacheConfigurations.put("getHotDeal", getHotDealConfig);
        cacheConfigurations.put("getHotDealProducts", getHotDealProductsConfig);
        cacheConfigurations.put("getHotDealProduct", getHotDealProductConfig);

        return RedisCacheManager.builder(redisConnectionFactory)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

}
