package com.hong.hotdealservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.kafka.ExpiredOrderEventDto;
import com.hong.common.dto.kafka.RefundOrderEventDto;
import com.hong.common.dto.kafka.StockFinalizeEventDto;
import com.hong.hotdealservice.service.HotDealProductStockService;
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
    private final HotDealProductStockService hotDealProductStockService;

    // 결제, 주문 내역 처리 후 재고 최종 반영
    // Kafka Retry, Dlt 설정
    @RetryableTopic(
            attempts = "4", // 재시도 3회, 총 4번
            backoff = @Backoff(delay = 3000L, multiplier = 2) // 간격 3초 이후 2배씩
    )
    @KafkaListener(
            topics = "${kafka.topics.order-payment-result}",
            groupId = "${kafka.group-id.hotdeal-service-stock-final}",
            containerFactory = "customKafkaListenerContainerFactory")
    public void consumeOrderPaymentResult(String eventData){
        try{
            // 1. String 형태의 Json -> 역직렬화
            StockFinalizeEventDto stockFinalizeEventDto = objectMapper.readValue(eventData, StockFinalizeEventDto.class);

            // 2. 주무 결과에 따른 재고 최종 처리
            hotDealProductStockService.handleStockFinalization(stockFinalizeEventDto);
            log.info("주문 상태 변경 처리 완료. orderId : {}", stockFinalizeEventDto.getOrderId());
        }
        catch (JsonProcessingException e){
            log.error("JSON 역직렬화 예외 발생", e);
            throw new RuntimeException("JSON 처리 오류로 재시도");
        }
    }

    // 만료된 주문 재고 점유 해제
    // Kafka Retry, Dlt 설정
    @RetryableTopic(
            attempts = "4", // 재시도 3회, 총 4번
            backoff = @Backoff(delay = 3000L, multiplier = 2) // 간격 3초 이후 2배씩
    )
    @KafkaListener(
            topics = "${kafka.topics.expired-order}",
            groupId = "${kafka.group-id.hotdeal-service-expired-order}",
            containerFactory = "customKafkaListenerContainerFactory")
    public void consumeExpiredOrder(String eventData){
        try{
            // 1. String 형태의 Json -> 역직렬화
            ExpiredOrderEventDto expiredOrderEventDto = objectMapper.readValue(eventData, ExpiredOrderEventDto.class);

            // 2. 만료된 주문 재고 점유 해제
            hotDealProductStockService.releaseReservedStocks(expiredOrderEventDto);
            log.info("만료된 주문 재고 점유 해제 완료. orderId = {}", expiredOrderEventDto.getOrderId());
        }
        catch (JsonProcessingException e){
            log.error("JSON 역직렬화 예외 발생", e);
            throw new RuntimeException("JSON 처리 오류로 재시도");
        }
    }


    // 환불 처리 된 주문에 의한 재고 롤백
    // Kafka Retry, Dlt 설정
    @RetryableTopic(
            attempts = "4", // 재시도 3회, 총 4번
            backoff = @Backoff(delay = 3000L, multiplier = 2) // 간격 3초 이후 2배씩
    )
    @KafkaListener(
            topics = "${kafka.topics.refund-order}",
            groupId = "${kafka.group-id.hotdeal-service-refund-order}",
            containerFactory = "customKafkaListenerContainerFactory")
    public void consumeRefundOrder(String eventData){
        try{
            // 1. String 형태의 Json -> 역직렬화
            RefundOrderEventDto refundOrderEventDto = objectMapper.readValue(eventData, RefundOrderEventDto.class);

            // 2. 재고 복구
            hotDealProductStockService.restoreStock(refundOrderEventDto);
            log.info("환불된 주문으로 이한 재고 복구 완료. orderId = {}", refundOrderEventDto.getOrderId());
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
