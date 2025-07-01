package com.hong.orderservice.scheduler.outbox;

import com.hong.orderservice.domain.outbox.UserCartOutbox;
import com.hong.orderservice.domain.status.OutboxStatus;
import com.hong.orderservice.repository.outbox.UserCartOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Profile("!test")
@Service
@RequiredArgsConstructor
@Slf4j(topic = "[UserCartOutboxScheduler]")
@Transactional
public class UserCartOutboxScheduler {
    private final UserCartOutboxEventRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 3_000) // 30초 주기
    @SchedulerLock(
            name = "UserCartOutboxScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "30s")
    public void publishOutbox(){
        // 1. outbox DB 조회(오래된 데이터부터 가져와서 순서 보장)
        List<OutboxStatus> statuses = List.of(OutboxStatus.FAILED, OutboxStatus.PENDING);
        List<UserCartOutbox> foundOutboxes = outboxRepository.findByOutboxStatusInOrderByIdAsc(statuses);

        // 2. 조회된 outbox FeignClient 이벤트 발행(비동기 처리)
        if(!foundOutboxes.isEmpty()){
            log.info("User Cart 정리 outbox {} 건 진행중", foundOutboxes.size());

            for (UserCartOutbox outbox : foundOutboxes) {
                // Status = IN_PROGRESS, tryCount++ 처리;
                outbox.updateToInProgress();
                // 이벤트 발행
                eventPublisher.publishEvent(outbox);
            }
        }
    }
}
