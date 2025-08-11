package com.hong.orderservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.kafka.PaymentResultEventDto;
import com.hong.orderservice.service.OrderApiService;
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
    private final OrderApiService orderApiService;

    // Kafka Retry, Dlt 설정
    @RetryableTopic(
            attempts = "4", // 재시도 3회, 총 4번
            backoff = @Backoff(delay = 3000L, multiplier = 2) // 간격 3초 이후 2배씩
    )
    @KafkaListener(
            topics = "${kafka.topics.payment-result}",
            groupId = "${kafka.group-id.order-service-payment-result}",
            containerFactory = "customKafkaListenerContainerFactory")
    public void consumePaymentResult(String eventData){
        try{
            // todo 추후 학제 ,,, 확인용
            log.info("dto = {}", eventData);
            // 1. String 형태의 Json -> 역직렬화
            PaymentResultEventDto paymentResultEventDto = objectMapper.readValue(eventData, PaymentResultEventDto.class);

            // 2. 결제 결과에 따른 order, delivery 상태 변경
            orderApiService.updateOrderAndDelivery(paymentResultEventDto);
            log.info("주문 상태 변경 처리 완료. orderId = {}", paymentResultEventDto.getOrderId());
        }
        catch (JsonProcessingException e){
            log.error("JSON 역직렬화 예외 발생", e);
            throw new RuntimeException("JSON 처리 오류로 재시도");
        }
    }

    @DltHandler
    public void handleDlt(String eventData){
        log.error("메시지 처리 재시도 모두 실패. DLT로 이동된 메시지 = {}", eventData);
    }

}
