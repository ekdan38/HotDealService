package com.hong.paymentservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.kafka.ExpiredOrderEventDto;
import com.hong.common.dto.kafka.RefundOrderEventDto;
import com.hong.paymentservice.service.PaymentApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "[kafkaMessageConsumer]")
public class KafkaMessageConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentApiService paymentApiService;

    // 만료된 주문에의한 결제 만료 처리
    // Kafka Retry, Dlt 설정
    @RetryableTopic(
            attempts = "4", // 재시도 3회, 총 4번
            backoff = @Backoff(delay = 3000L, multiplier = 2) // 간격 3초 이후 2배씩
    )
    @KafkaListener(
            topics = "${kafka.topics.expired-order}",
            groupId = "${kafka.group-id.payment-service-expired-order}",
            containerFactory = "customKafkaListenerContainerFactory")
    public void consumeExpiredOrder(String eventData){
        try{
            // todo 추후 학제 ,,, 확인용
            log.info("dto = {}", eventData);
            // 1. String 형태의 Json -> 역직렬화
            ExpiredOrderEventDto expiredOrderEventDto = objectMapper.readValue(eventData, ExpiredOrderEventDto.class);

            // 2. payment expired 처리
            paymentApiService.updateToExpired(expiredOrderEventDto);
        }
        catch (JsonProcessingException e){
            log.error("JSON 역직렬화 예외 발생", e);
            throw new RuntimeException("JSON 처리 오류로 재시도");
        }
    }

    // 환불된 주문에 대한 결제 취소 처리
    // Kafka Retry, Dlt 설정
    @RetryableTopic(
            attempts = "4", // 재시도 3회, 총 4번
            backoff = @Backoff(delay = 3000L, multiplier = 2) // 간격 3초 이후 2배씩
    )
    @KafkaListener(
            topics = "${kafka.topics.refund-order}",
            groupId = "${kafka.group-id.payment-service-refund-order}",
            containerFactory = "customKafkaListenerContainerFactory")
    public void consumeRefundOrder(String eventData){
        try{
            // todo 추후 학제 ,,, 확인용
            log.info("dto = {}", eventData);
            // 1. String 형태의 Json -> 역직렬화
            RefundOrderEventDto refundOrderEventDto = objectMapper.readValue(eventData, RefundOrderEventDto.class);

            // 2. payment canceled 처리
            paymentApiService.updtaeToCanceled(refundOrderEventDto);
        }
        catch (JsonProcessingException e){
            log.error("JSON 역직렬화 예외 발생", e);
            throw new RuntimeException("JSON 처리 오류로 재시도");
        }
    }

    @DltHandler
    public void handleDlt(String eventData){
        log.error("메시지 처리 재시도 모두 실패. DLT로 이동된 메시지: {}", eventData);
    }

}
