package com.hong.hotdealservice.service;

import com.hong.hotdealservice.domain.RedisStockSaveFail;
import com.hong.hotdealservice.repository.ProductRedisRepository;
import com.hong.hotdealservice.repository.RedisStockSaveFailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealStockRedisService]")
public class HotDealStockRedisService {

    private final ProductRedisRepository redisRepository;
    private final RedisStockSaveFailRepository redisStockSaveFailRepository;

    @Retryable(retryFor = {RedisConnectionFailureException.class,
            DataAccessResourceFailureException.class,
            RedisSystemException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public void saveStockInRedisWithRetry(Long hotDealProductId, Integer stock, LocalDateTime endTime){
        log.info("Redis 재고 동기화 시작. hotDealProductId = {}", hotDealProductId);
        if(stock > 0) {
            try{
                log.info("hp = {}, stock = {}, endTime = {}", hotDealProductId, stock, endTime);
                redisRepository.saveStockWithTTL(hotDealProductId, stock, endTime);
                log.info("Redis stock 저장 성공. hotDealProductId = {}, stock = {}", hotDealProductId, stock);

            }catch (Exception e){
                log.info("error = {}", e.getMessage());
                throw e;
            }
        }
        else{
            redisRepository.deleteStock(hotDealProductId);
            log.info("Redis stock 삭제 성공. hotDealProductId = {}", hotDealProductId);
        }
        log.info("Redis 재고 동기화 완료. hotDealProductId = {}", hotDealProductId);
    }

    @Recover
    private void saveStockInRedisRecover(Long hotDealProductId, Integer stock, LocalDateTime endTime, Exception e) {
        log.error("hotDealProduct stock 최종 Redis 저장 실패. Recover 호출. hotDealProductId = {}, reason = {}", hotDealProductId, e.getMessage());
        RedisStockSaveFail redisStockSaveFail = RedisStockSaveFail.create(hotDealProductId, endTime, stock, e.getMessage());
        redisStockSaveFailRepository.save(redisStockSaveFail);
    }
}
