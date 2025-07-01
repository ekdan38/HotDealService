package com.hong.paymentservice.scheduler;

import com.hong.paymentservice.domain.outbox.OrderStatusOutbox;
import com.hong.paymentservice.domain.status.OutboxStatus;
import com.hong.paymentservice.repository.OrderStatusOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Profile("!test")
@Component
@Transactional
@RequiredArgsConstructor
@Slf4j(topic = "[OrderStatusOutboxScheduler]")
public class OrderStatusOutboxScheduler {
    private final OrderStatusOutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 3_000) // 3초 주기
    @SchedulerLock(
            name = "OrderStatusOutboxScheduler",
            lockAtMostFor = "10s",
            lockAtLeastFor = "3s")
    public void publishOutbox(){
        log.info("OrderStatusOutboxScheduler 실행");
        // 1. outbox DB 조회(오래된 데이터부터 가져와서 순서 보장)
        List<OutboxStatus> statuses = List.of(OutboxStatus.FAILED, OutboxStatus.PENDING);
        List<OrderStatusOutbox> foundOutboxes = outboxRepository.findByOutboxStatusInOrderByIdAsc(statuses);

        // 2. 조회된 outbox FeignClient 이벤트 발행(비동기 처리)
        if(!foundOutboxes.isEmpty()){
            log.info("Order Status 변경 outbox {} 건 진행중", foundOutboxes.size());

            for (OrderStatusOutbox outbox : foundOutboxes) {
                // Status = IN_PROGRESS, tryCount++ 처리;
                outbox.updateToInProgress();
                eventPublisher.publishEvent(outbox);
            }
        }
        log.info("OrderStatusOutboxScheduler 종료");
    }
}
