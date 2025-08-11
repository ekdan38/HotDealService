package com.hong.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.kafka.ExpiredOrderEventDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.common.status.AggregateType;
import com.hong.common.status.EventType;
import com.hong.common.status.OutboxDeliveryMethod;
import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.Outbox;
import com.hong.orderservice.event.OutboxEvent;
import com.hong.orderservice.event.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[ExpiredOrderService]")
public class ExpiredOrderService {

    private final ApplicationEventPublisher eventPublisher;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleExpiredOrder(Order order) {
        // Order.Status = EXPIRED 변경
        order.updateToExpired();

        // 재고 점유 해제, 결제 만료 위한 Outbox 생성 및 저장
        Outbox outbox = saveOutbox(order, OutboxDeliveryMethod.KAFKA);

        // 이벤트 발행
        eventPublisher.publishEvent(new OutboxEvent(outbox));

        log.info("만료된 주문 처리 완료. OrderId = {}", order.getId());
    }

    private Outbox saveOutbox(Order order, OutboxDeliveryMethod deliveryMethod){
        try{
            ExpiredOrderEventDto payloadDto = new ExpiredOrderEventDto(order.getId());
            String payload = objectMapper.writeValueAsString(payloadDto);
            return outboxService.save(Outbox.create(AggregateType.ORDER, order.getId(), EventType.EXPIRED_ORDER, deliveryMethod, payload));

        }catch (Exception e){
            log.error("만료된 주문 Outbox 생성 중 오류 발생. errorMessage = {}", e.getMessage());
            throw new OrderException(ErrorCode.ORDER_INTERNAL_SERVER_ERROR);
        }
    }

}
