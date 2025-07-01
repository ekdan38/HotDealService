package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.dto.HotDealCacheDto;
import com.hong.hotdealservice.dto.HotDealPagingCacheDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class HotDealRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final long TTL_SECONDS = 300L;

    private String createHotDealKey(Long hotDealId){
        return "getHotDeal::hotdeals:" + hotDealId;
    }

    private String createHotDealPagingKey(Long cursor, int size, String search){
        return "getHotDeals::hotdeals:cursor:" + (cursor == null ? "" : cursor)
                + ":size:" + size + ":search:" + (search == null ? "" : search);
    }

    // hotDeal set
    public void saveWithTTL(HotDealCacheDto hotDeal){
        String key = createHotDealKey(hotDeal.getId());
        redisTemplate.opsForValue().set(key, hotDeal, Duration.ofMinutes(30));
    }

    // hotDeal multiSet
    public void saveAllWithTTL(List<HotDealCacheDto> hotDeals){
        // 1. multiSet
        Map<String, Object> newCacheEntries = new HashMap<>();
        for (HotDealCacheDto dto : hotDeals) {
            String key = createHotDealKey(dto.getId());
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

    // hotDeal Get
    public HotDealCacheDto findById(Long hotDealId){
        String key = createHotDealKey(hotDealId);
        Object cachedData = redisTemplate.opsForValue().get(key);
        return (HotDealCacheDto) cachedData;
    }

    // hotDeal multiGet
    public List<HotDealCacheDto> findAllByIds(List<Long> hotDealIds){
        List<String> keys = hotDealIds.stream()
                .map(this::createHotDealKey)
                .toList();
        List<Object> cachedData = redisTemplate.opsForValue().multiGet(keys);
        return cachedData.stream()
                .map(o -> (HotDealCacheDto) o)
                .collect(Collectors.toList());
    }

    // hotDealPaging Get
    public HotDealPagingCacheDto findPagingByCursorAndSizeAndSearch(Long cursor, int size, String search){
        String key = createHotDealPagingKey(cursor, size, search);
        return (HotDealPagingCacheDto)redisTemplate.opsForValue().get(key);
    }

    // hotDeal delete (부분 무효화)
    public void deleteById(Long hotDealId){
        String key = createHotDealKey(hotDealId);
        redisTemplate.delete(key);
    }
}
