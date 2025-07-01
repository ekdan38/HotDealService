package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.dto.ProductCacheDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductRedisRepository {

    private final RedisTemplate<String, Object>redisTemplate;
    private static final long TTL_SECONDS = 300L;

    private String createHotDealProductKey(Long productId){
        return "getProduct::products:" + productId;
    }
    private String createStockKey(Long productId){
        return "product:" + productId + ":stock";
    }

    // hotDealProduct stock Set
    public void saveStockWithTTL(Long productId, Integer stock, LocalDateTime hotDealEndTime){
        String key = createStockKey(productId);
        LocalDateTime now = LocalDateTime.now();
        // TTL 계산 => 종료 시간 + 10분 - 현재 시간
        Duration ttl = Duration.between(now, hotDealEndTime.plusMinutes(10));
        long ttlSeconds = ttl.getSeconds();
        redisTemplate.opsForValue().set(key, stock, ttlSeconds, TimeUnit.SECONDS);
    }

    // hotDealProduct stock delete
    public void deleteStock(Long productId){
        String key = createStockKey(productId);
        redisTemplate.delete(key);
    }

    public Integer getStock(Long productId){
        String key = createStockKey(productId);
        return (int) redisTemplate.opsForValue().get(key);
    }

    public Long increaseStock(Long productId, Integer quantity){
        String key = createStockKey(productId);
        return redisTemplate.opsForValue().increment(key, quantity);
    }

    // hotDealProduct stock get
    public Long decreaseStockWithLua(Long productId, Integer quantity){
        String key = createStockKey(productId);
        String luaScript = """
            local currentStock = tonumber(redis.call('get', KEYS[1]))
            if currentStock == nil then
                return -1
            end
            if currentStock < tonumber(ARGV[1]) then
                return -1
            end
            return redis.call('decrby', KEYS[1], ARGV[1])
        """;
        return redisTemplate.execute(
                (RedisCallback<Long>) connection -> connection.eval(
                        luaScript.getBytes(),
                        ReturnType.INTEGER,
                        1,
                        key.getBytes(),
                        String.valueOf(quantity).getBytes()
                )
        );
    }

    // product set
    public void saveWithTTL(ProductCacheDto product){
        String key = createHotDealProductKey(product.getProductId());
        redisTemplate.opsForValue().set(key, product, Duration.ofMinutes(30));
    }

    // hotDealProduct multiSet
    public void saveAllWithTTL(List<ProductCacheDto> products){
        // 1. multiSet
        Map<String, Object> newCacheEntries = new HashMap<>();
        for (ProductCacheDto dto : products) {
            String key = createHotDealProductKey(dto.getProductId());
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

    public ProductCacheDto getProductById(Long productId){
        String key = createHotDealProductKey(productId);
        Object cachedData = redisTemplate.opsForValue().get(key);
        return (ProductCacheDto) cachedData;
    }

    // hotDealProduct multiGet
    public List<ProductCacheDto> findAllByIds(List<Long> hotDealProductIds){
        List<String> keys = hotDealProductIds.stream()
                .map(this::createHotDealProductKey)
                .toList();
        List<Object> cachedData = redisTemplate.opsForValue().multiGet(keys);
        return cachedData.stream()
                .map(o -> (ProductCacheDto) o)
                .collect(Collectors.toList());
    }

    // hotDealProduct delete (부분 무효화)
    public void deleteAllProductByIds(List<Long> hotDealProductIds){
        List<String> keys = hotDealProductIds.stream()
                .map(this::createHotDealProductKey)
                .collect(Collectors.toList());
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // cacheable getHotDealProduct delete (전면 무효화)
    public void deleteAllGetProductsKeys(){
        Set<String> keys = redisTemplate.keys("getHotDealProducts::hotdeal:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
