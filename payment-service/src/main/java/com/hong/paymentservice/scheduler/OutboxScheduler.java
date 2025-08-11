package com.hong.paymentservice.scheduler;

import com.hong.common.status.OutboxDeliveryMethod;
import com.hong.common.status.OutboxStatus;
import com.hong.paymentservice.domain.Outbox;
import com.hong.paymentservice.event.KafkaMessageProducer;
import com.hong.paymentservice.event.OutboxEvent;
import com.hong.paymentservice.event.OutboxService;
import com.hong.paymentservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Profile("!test")
@Component
@RequiredArgsConstructor
@Slf4j(topic = "[OutboxScheduler]")
public class OutboxScheduler {
    private final OutboxRepository outboxRepository;
    private final OutboxService outboxService;
    private final KafkaMessageProducer kafkaMessageProducer;

    private static final int MAX_RETRY_COUNT = 6;
    private static final int PENDING_TIMEOUT_MINUTES = 5;

    @Scheduled(fixedDelay = 3_0000) // 30초 주기
    @SchedulerLock(
            name = "OutboxScheduler",
            lockAtMostFor = "10s",
            lockAtLeastFor = "3s")
    public void publishOutbox(){
        // 1. Outbox.Status = FAILED, PENDING(PENDING_TIME_OUT 지난) 조회
        LocalDateTime cutOffTime = LocalDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES);
        List<Outbox> foundOutboxes =
                outboxRepository.findByOutboxStatusInAndCreatedAtBefore(
                        List.of(OutboxStatus.FAILED, OutboxStatus.PENDING), cutOffTime
                );

        // 2. 처리해야할 Outbox 존재하지 않으면 return
        if(foundOutboxes.isEmpty()) return;

        // 3. Outbox 처리
        log.info("스케줄러에 의한 재시도 할 OutboxEvent {} 건 조회 완료.", foundOutboxes.size());
        for (Outbox outbox : foundOutboxes) {
            //  Propagation.REQUIRES_NEW 트랜잭션으로 Outbox.Status = IN_PROGRESS 변경
            outboxService.updateToInProgress(outbox.getId());

            // retry 횟수 넘은 Outbox 처리
            boolean isAborted = handleOverThanRetryCount(outbox);

            // retry 횟수 넘지 않으면 재시도 처리
            OutboxDeliveryMethod deliveryMethod = outbox.getDeliveryMethod();
            if(!isAborted){
                // Feign
                if(deliveryMethod.equals(OutboxDeliveryMethod.FEIGN)){
                    //ExternalServiceInvoker 로 처리 요청
                }
                // Kafka
                else if(deliveryMethod.equals(OutboxDeliveryMethod.KAFKA)){
                    // KafkaMessageProducer 로 처리 요청
                    kafkaMessageProducer.produceMessage(new OutboxEvent(outbox));
                }
            }
        }
    }

    // retry 횟수 넘은 Outbox 처리
    private boolean handleOverThanRetryCount(Outbox outbox){
        Integer retryCnt = outbox.getTryCount();
        // 최대 재시도 실패
        if(retryCnt >= MAX_RETRY_COUNT){
            // Outbox.Status = ABORTED 변경
            outboxService.updateToAborted(outbox.getId());
            return true;
        }
        return false;
    }
}
