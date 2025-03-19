package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.dto.HotDealProductCacheDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class HotDealProductRedisRepository {

    private final RedisTemplate<String, Object>redisTemplate;
    private static final long TTL_SECONDS = 300L;

    private String createHotDealProductKey(Long hotDealProductId){
        return "getHotDealProduct::hotdeal_products:" + hotDealProductId;
    }

    // hotDealProduct multiSet
    public void saveAllHotDealProductWithTTL(List<HotDealProductCacheDto> hotDealProductCacheDtos){
        // 1. multiSet
        Map<String, Object> newCacheEntries = new HashMap<>();
        for (HotDealProductCacheDto dto : hotDealProductCacheDtos) {
            String key = createHotDealProductKey(dto.getHotDealProductId());
            newCacheEntries.put(key, dto);
        }
        redisTemplate.opsForValue().multiSet(newCacheEntries);

        // 2. TTL 설정 (Redis PipeLine)
        redisTemplate.executePipelined((RedisCallback<Void>) connection -> {
            for (String key : newCacheEntries.keySet()) {
                connection.expire(key.getBytes(), TTL_SECONDS);
            }
            return null;
        });
    }

    // hotDealProduct multiGet
    public List<HotDealProductCacheDto> findAllHotDealProductByIds(List<Long> hotDealProductIds){
        List<String> keys = hotDealProductIds.stream()
                .map(this::createHotDealProductKey)
                .toList();
        List<Object> cachedData = redisTemplate.opsForValue().multiGet(keys);
        return cachedData.stream()
                .map(o -> (HotDealProductCacheDto) o)
                .collect(Collectors.toList());
    }

    // hotDealProduct delete (부분 무효화)
    public void deleteAllHotDealProductByIds(List<Long> hotDealProductIds){
        List<String> keys = hotDealProductIds.stream()
                .map(this::createHotDealProductKey)
                .collect(Collectors.toList());
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // cacheable getHotDealProduct delete (전면 무효화)
    public void deleteAllGetHotDealProductsKeys(){
        Set<String> keys = redisTemplate.keys("getHotDealProducts::hotdeal:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
