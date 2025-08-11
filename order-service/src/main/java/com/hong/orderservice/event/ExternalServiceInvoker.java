package com.hong.orderservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.PaymentCreateRequestDto;
import com.hong.common.dto.PaymentCreateResponseDto;
import com.hong.common.status.EventType;
import com.hong.orderservice.client.payment.Resilience4JPaymentServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "[ExternalServiceInvoker]")
public class ExternalServiceInvoker {

    private final Resilience4JPaymentServiceClient resilience4JPaymentServiceClient;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public void invokeFeignService(OutboxEvent event){
        EventType eventType = event.getEventType();
        Long outboxId = event.getOutboxId();

        try{
            // EventType에 따른 처리
            // PaymentCreate 요청 처리
            if(eventType == EventType.PAYMENT_CREATE){
                // 1. Event 의 payload 에서 payload => requestDto 변환
                PaymentCreateRequestDto requestDto = objectMapper.readValue(event.getPayload(), PaymentCreateRequestDto.class);
                // 2. FeignClient 요청
                PaymentCreateResponseDto responseDto = resilience4JPaymentServiceClient.createPayment(requestDto);
                // 3. FeignClient 성공/실패 처리
                handleFeignResponse(outboxId, eventType, responseDto.isSuccess(), responseDto.isRetriable());
            }
        }
        // 처리할 수 없는 예외 발생
        catch (Exception e){
            handleException(outboxId, eventType, e.getMessage());
        }
    }

    // FeignClient 성공/실패 처리
    private void handleFeignResponse(Long outboxId, EventType eventType, boolean success, boolean retriable){
        // 성공
        if(success){
            // Outbox Status => PUBLISHED, publishedAt => now
            log.info("OutboxEvent Feign 처리 완료. PUBLISHED 처리. OutboxId = {}, EventType = {}", outboxId, eventType);
            outboxService.updateToPublished(outboxId);
        }
        // 실패 Retry, CircuitBreaker Fallback
        else if(retriable){
            // Outbox Status => FAILED, tryCnt++;
            log.error("OutboxEvent Feign 처리 실패. Failed 처리. OutboxId = {}, EventType = {}", outboxId, eventType);
            outboxService.updateToFailed(outboxId);
        }
    }

    // 처리할 수 없는 예외 처리 Catch 블록
    private void handleException(Long outboxId, EventType eventType, String errorMessage){
        // Outbox Status => FAILED, tryCnt++;
        log.error("OutboxEvent Feign 처리 실패. 예외 발생. Failed 처리. OutboxId = {}, EventType = {}, errorMessage = {}"
                , outboxId, eventType, errorMessage);
        outboxService.updateToFailed(outboxId);
    }
}
