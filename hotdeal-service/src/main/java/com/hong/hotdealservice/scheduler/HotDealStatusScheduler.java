package com.hong.hotdealservice.scheduler;

import com.hong.hotdealservice.repository.HotDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Profile("!test")
@Service
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealScheduler]")
public class HotDealStatusScheduler {

    private final HotDealRepository hotDealRepository;

    /**
     * HotDeal 에 대한 status 변경 스케쥴러
     * 주문 시에 사용자는 startTime, endTime 기준 주문 가능 한지
     * status != EXPIRED, isDeleted 로 주문 가능 여부 검증
     * 따라서 해당 스케쥴러는 보조적인 status 변경 용도
     */
    @Transactional
    @SchedulerLock(
            name = "HotDealStatusScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "1m"
    )
    @Scheduled(fixedRate = 300000)
    public void updateHotDealStatus() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        // hotDeal status 변경 (ACTIVE)
        int toActive = hotDealRepository.updateScheduledToActive(now);
        log.info("HotDealStatus ACTIVE 로 상태 변경. 변경된 HotDeal 수 = {}", toActive);
        // hotDeal status 변경 (EXPIRED)
        int toExpired = hotDealRepository.updateScheduledToExpired(now);
        log.info("HotDealStatus EXPIRED 로 상태 변경. 변경된 HotDeal 수 = {}", toExpired);
    }
}
