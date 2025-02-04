package com.hong.hotdealservice.scheduler;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.BusinessException;
import com.hong.hotdealservice.repository.HotDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealScheduler]")
public class HotDealScheduler {

    private final HotDealRepository hotDealRepository;
    private final RedissonClient redissonClient;
    private final Environment env;

    // 5 분 마다 실행
    // 어떠한 인스턴스가 스케쥴링을 시작하면 다른 인스턴스는 스케쥴링을 돌리지 않도록 한다.
    // => waitTime = 0, leaseTime = -1 로 설정하여 먼저 스케쥴링을 시도한 인스턴스만 스케쥴링을 처리힌다.
    // => 이후 나머지 인스턴스는 스케쥴링을 돌리지 않는다.
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void updateHotDealStatus(){
        String instanceId = env.getProperty("eureka.instance.instance-id");
        String lockKey = "hotDeal_status_lock:";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean isLocked = lock.tryLock(0, -1, TimeUnit.SECONDS);
            if (!isLocked) {
                log.info("다른 인스턴스가 스케쥴링을 진행중입니다.");
            }
            log.info("락 획득 성공 key = {}", lockKey);
            log.info("{} 인스턴스가 스케쥴링을 진행합니다.", instanceId);

            LocalDateTime now = LocalDateTime.now();

            // hotDeal status 변경 (ACTIVE)
            hotDealRepository.updateScheduledToActive(now);
            log.info("Scheduler ACTIVE 로 상태 변경");

            // hotDeal status 변경 (EXPIRED)
            hotDealRepository.updateScheduledToExpired(now);
            log.info("Scheduler EXPIRED 로 상태 변경");

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    lock.unlock();
                    log.info("락 해제 완료 key = {}", lockKey);
                }
            });
            log.info("{} 인스턴스가 스케쥴링을 종료 했습니다.", instanceId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("hotDeal_status 에 대한 락 획득 중 입터럽트가 발생했습니다.");
            throw new BusinessException(ErrorCode.HOTDEAL_LOCK_INTERRUPTED);
        }
    }
}
