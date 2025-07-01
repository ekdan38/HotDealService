package com.hong.orderservice.scheduler.outbox;

import com.hong.orderservice.domain.outbox.CreatePaymentOutbox;
import com.hong.orderservice.domain.status.OutboxStatus;
import com.hong.orderservice.repository.outbox.CreatePaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j(topic = "[CreatePaymentScheduler]")
@Transactional
public class CreatePaymentScheduler {
    private final CreatePaymentOutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 3_000) // 3초 주기
    @SchedulerLock(
            name = "CreatePaymentScheduler",
            lockAtMostFor = "10s",
            lockAtLeastFor = "3s")
    public void publishOutbox(){
        // 1. outbox DB 조회(오래된 데이터부터 가져와서 순서 보장)
        List<OutboxStatus> statuses = List.of(OutboxStatus.FAILED, OutboxStatus.PENDING);
        List<CreatePaymentOutbox> foundOutboxes = outboxRepository.findByOutboxStatusInOrderByIdAsc(statuses);

        // 2. 조회된 outbox FeignClient 이벤트 발행(비동기 처리)
        if(!foundOutboxes.isEmpty()){
            log.info("Payment 생성 outbox {} 건 진행중", foundOutboxes.size());

            for (CreatePaymentOutbox outbox : foundOutboxes) {
                // Status = IN_PROGRESS, tryCount++ 처리;
                outbox.updateToInProgress();
                // 이벤트 발행
                eventPublisher.publishEvent(outbox);
            }
        }

    }
}
