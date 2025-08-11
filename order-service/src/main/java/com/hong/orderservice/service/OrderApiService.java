package com.hong.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.OrderFetchRequestDto;
import com.hong.common.dto.OrderFetchResponseDto;
import com.hong.common.dto.OrderUpdateResponseDto;
import com.hong.common.dto.kafka.PaymentResultEventDto;
import com.hong.common.dto.kafka.StockFinalizeEventDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.common.status.AggregateType;
import com.hong.common.status.EventType;
import com.hong.common.status.OutboxDeliveryMethod;
import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.Outbox;
import com.hong.orderservice.event.OutboxEvent;
import com.hong.orderservice.event.OutboxService;
import com.hong.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j(topic = "[OrderApiService]")
public class OrderApiService {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;
    private final ApplicationEventPublisher eventPublisher;

    // Order 조회
    public OrderFetchResponseDto fetchOrder(OrderFetchRequestDto requestDto) {
        // 1. order 조회, 검증
        Order order = fetchOrderAndValidate(requestDto.getOrderId(), requestDto.getUserId());
        // 2. 응답 Dto 변환
        return convertToOrderFetchResponse(order);
    }

    // 결제 결과 기반 order, delivery 상태 변경
    @Transactional
    public OrderUpdateResponseDto updateOrderAndDelivery(PaymentResultEventDto eventDto) {
        // 1. order 조회 (delivery fetch join) 및 검증
        Order order = getOrderWithDeliveryAndValidate(eventDto);

        // 2. 결제 결과 기반 order, delivery 상태 변경
        // 결제 성공 처리
        if (eventDto.isSuccess()) {
            order.updateToPaymentSuccess(eventDto.getPaidAt());
            log.info("결제 성공에 다른 주문 상태 변경 완료. orderId = {}, 재고 최종 반영 이벤트 발행", order.getId());
            // outbox 생성, 재고 확정 처리
            Outbox outbox = saveOutbox(order, OutboxDeliveryMethod.KAFKA, true);
            // 이벤트 발행
            eventPublisher.publishEvent(new OutboxEvent(outbox));
        }
        // 결제 실패 처리
        else {
            order.updateToPaymentFailed();
            log.info("결제 실패에 다른 주문 상태 변경 완료. orderId = {}, 재고 최종 반영 이벤트 발행", order.getId());
            // outbox 생성, 점유테이블 롤백(보상 트랜잭션)
            Outbox outbox = saveOutbox(order, OutboxDeliveryMethod.KAFKA, false);
            // 이벤트 발행
            eventPublisher.publishEvent(new OutboxEvent(outbox));
        }

        // 3. 응답 Dto 반환
        return new OrderUpdateResponseDto(true);
    }

    private OrderFetchResponseDto convertToOrderFetchResponse(Order order) {
        return new OrderFetchResponseDto(order.getUserId(), order.getId(), order.getAmount(), order.getStatus().name());
    }

    private Order getOrderWithDeliveryAndValidate(PaymentResultEventDto eventDto) {
        return orderRepository.findByIdAndUserIdWithDelivery(eventDto.getOrderId()).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. orderId = {}", eventDto.getOrderId());
            return new OrderException(ErrorCode.ORDER_NOT_FOUND, eventDto.getOrderId());
        });
    }

    // order 조회, 검증
    private Order fetchOrderAndValidate(String orderId, Long userId) {
         return orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. orderId = {}, userId = {}", orderId, userId);
            return new OrderException(ErrorCode.ORDER_NOT_FOUND, orderId, userId);
        });
    }

    // outbox save
    private Outbox saveOutbox(Order order, OutboxDeliveryMethod deliveryMethod, boolean result){
        try{
            StockFinalizeEventDto payloadDto = new StockFinalizeEventDto(order.getId(), order.getUserId(), result);
            String payload = objectMapper.writeValueAsString(payloadDto);
            return outboxService.save(Outbox.create(AggregateType.ORDER, order.getId(), EventType.ORDER_PAYMENT_RESULT, deliveryMethod, payload));

        }
        catch (Exception e){
            log.error("결제 결과에 따른 주문 상태 변경 중 오류 발생. errorMessage = {}", e.getMessage());
            throw new OrderException(ErrorCode.ORDER_INTERNAL_SERVER_ERROR);
        }
    }
}
