package com.hong.orderservice.event;

import com.hong.common.status.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "[KafkaMessageProducer]")
public class KafkaMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxService outboxService;

    public void produceMessage(OutboxEvent event){
        EventType eventType = event.getEventType();
        Long outboxId = event.getOutboxId();
        String payload = event.getPayload();

        // topic 결정
        String topic = eventType.getTopic();

        try{
            // Kafka produce, 결과 callback 처리
            produceEventAndHandleResult(topic, payload, outboxId, eventType);
        }
        // send() 동기 예외 처리
        catch (Exception e){
            handleException(outboxId, eventType, e.getMessage());
        }
    }

    // Kafka produce, 결과 callback 처리
    private void produceEventAndHandleResult(String topic, String payload, Long outboxId, EventType eventType){
        kafkaTemplate.send(topic, payload).whenComplete((result, ex) -> {
            // 이 내부에서 kafka retries 실행 => delivery.timeout.ms 내에서
            // 현재 enable.idempotence = true
            // 성공
            if(ex == null){
                // Outbox Status => PUBLISHED, publishedAt => now
                log.info("OutboxEvent Kafka produce 완료. PUBLISHED 처리. OutboxId = {}, EventType = {}", outboxId, eventType);
                outboxService.updateToPublished(outboxId);
            }
            // 실패 => Kafka Retry 모두 소진 or deliveryTimeout or broker로 전송 과정 예외
            else{
                // Outbox Status => FAILED, tryCnt++;
                log.error("OutboxEvent Kafka produce 실패. Failed 처리. OutboxId = {}, EventType = {}", outboxId, eventType);
                outboxService.updateToFailed(outboxId);
            }
        });
    }

    // send 동기 예외 처리 Catch 블록
    private void handleException(Long outboxId, EventType eventType, String errorMessage){
        log.error("OutboxEvent Kafka produce 실패. 예외 발생. Failed 처리. OutboxId = {}, EventType = {}, errorMessage = {}"
                , outboxId, eventType, errorMessage);
        outboxService.updateToFailed(outboxId);
    }

}
