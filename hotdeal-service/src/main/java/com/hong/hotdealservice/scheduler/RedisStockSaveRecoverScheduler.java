package com.hong.hotdealservice.scheduler;

import com.hong.hotdealservice.domain.RedisStockSaveFail;
import com.hong.hotdealservice.repository.ProductRedisRepository;
import com.hong.hotdealservice.repository.RedisStockSaveFailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Profile("!test")
@Service
@RequiredArgsConstructor
@Slf4j(topic = "[RedisStockSaveRecoverScheduler]")
public class RedisStockSaveRecoverScheduler {

    private final RedisStockSaveFailRepository failRepository;
    private final ProductRedisRepository redisRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 에 stock 저장 실패한 건 처리
    @Transactional
    @SchedulerLock(
            name = "RedisStockRecoverScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "1m"
    )
    @Scheduled(fixedDelay = 300000) // 5분마다 실행
    public void retryFailedRedisStockSaves() {
        List<RedisStockSaveFail> failedStocks = failRepository.findAll();

        // 1. Redis 연결 상태 확인
        if(!isRedisAvailable()){
            log.error("Redis 가 사용 불가입니다. 복구 작업을 진행하지 않습니다.");
            return;
        }

        // 2. 실패 건 없음
        if (failedStocks.isEmpty()) {
            log.info("hotDealProduct stock Redis 저장 실패 건 없음");
            return;
        }

        // 3. Redis 에 stock 저장 실패 건 처리
        failedStocks.forEach(fail -> {
            redisRepository.saveStockWithTTL(fail.getHotDealProductId(), fail.getStock(), fail.getEndTime());
            failRepository.delete(fail);
            log.info("Redis 저장 재시도 성공 - hotDealProductId: {}", fail.getHotDealProductId());
        });
    }

    private boolean isRedisAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
