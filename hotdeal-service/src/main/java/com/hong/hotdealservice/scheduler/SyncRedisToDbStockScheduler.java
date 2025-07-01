package com.hong.hotdealservice.scheduler;

import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.repository.HotDealRepository;
import com.hong.hotdealservice.repository.ProductRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Profile("!test")
@Service
@RequiredArgsConstructor
@Slf4j(topic = "[SyncRedisToDbStockScheduler]")
public class SyncRedisToDbStockScheduler {

    private final HotDealRepository hotDealRepository;
    private final ProductRedisRepository redisRepository;

    // Redis 재고 DB 재고 동기화 스케쥴러
    @Transactional
    @SchedulerLock(
            name = "SyncRedisToDbStockScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "1m"
    )
    @Scheduled(fixedDelay = 300000) // 5분마다 실행
    public void syncRedisStockToDb() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        List<HotDeal> hotDeals = hotDealRepository.findRecentlyEndedHotDeals(now.minusMinutes(5), now);

        for (HotDeal hotDeal : hotDeals) {
            for (HotDealProduct product : hotDeal.getHotDealProducts()) {
                Long productId = product.getId();
                Integer redisStock = redisRepository.getStock(productId);

                if (redisStock == null) {
                    log.error("Redis 에 재고가 없습니다. productId = {}", productId);
                    continue;
                }

                // 재고 동기화
                Integer dbStock = product.getStock();
                if (!redisStock.equals(dbStock)) {
                    product.syncStock(redisStock);
                    log.info("재고 동기화 productId={}, dbStock = {}, redisStock ={}", productId, dbStock, redisStock);
                }
            }
            // 재고 동기화 여부 마킹
            hotDeal.syncRedisStock(now);
        }
    }
}