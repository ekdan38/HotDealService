package com.hong.orderservice.facade;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[RedissonLockService]")
public class RedissonLockService {

    private final RedissonClient redissonClient;

    /**
     *
     * @param lockKey 락 키
     * @param waitTime 최대 대기 시간 (초)
     * @param leaseTime 락 유지 시간 (초)
     */
    // 락 획득 시도
    public RLock tryLock(String lockKey, Long waitTime, Long leaseTime) throws InterruptedException {
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

        if(!isLocked) {
            log.error("Lock acquisition failed for key = {} ", lockKey);
            throw new BusinessException(ErrorCode.WISHLIST_PRODUCT_NOT_FOUND);
        }
        return lock;
    }

    // 락 해제
    public void unLock(RLock lock){
        // lock 이 넘어오고, 현재 스레드가 락을 갖고 있나 확인
        if(lock != null && lock.isHeldByCurrentThread()){
            lock.unlock();
        }
    }
}
