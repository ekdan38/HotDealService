package com.hong.orderservice.service;

import com.hong.common.dto.OrderFetchRequestDto;
import com.hong.common.dto.OrderFetchResponseDto;
import com.hong.common.dto.OrderUpdateRequestDto;
import com.hong.common.dto.OrderUpdateResponseDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.outbox.StockConfirmOutbox;
import com.hong.orderservice.domain.outbox.UserCartOutbox;
import com.hong.orderservice.repository.OrderRepository;
import com.hong.orderservice.repository.outbox.StockConfirmOutboxRepository;
import com.hong.orderservice.repository.outbox.UserCartOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j(topic = "[OrderApiService]")
public class OrderApiService {

    private final OrderRepository orderRepository;
    private final StockConfirmOutboxRepository stockConfirmOutboxRepository;
    private final UserCartOutboxEventRepository userCartOutboxEventRepository;

    // Order 조회
    public OrderFetchResponseDto fetchOrder(OrderFetchRequestDto requestDto) {
        // 1. order 조회, 검증
        Order order = fetchOrderAndValidate(requestDto.getOrderId(), requestDto.getUserId());
        // 2. 응답 Dto 변환
        return convertToOrderFetchResponse(order);
    }

    // 결제 성공 여부 기반 order, delivery status update, user Cart 정리
    @Transactional
    public OrderUpdateResponseDto updateOrderAndDelivery(OrderUpdateRequestDto requestDto) {
        // 1. order 조회 (delivery fetch join) 및 검증
        Order order = getOrderWithDeliveryAndValidate(requestDto);

        // 2. 결제 성공 실패 처리(requestDto 로 결제 성공 유무)
        // 결제 성공 처리
        if (requestDto.isSuccess()){
            // order Status 변경
            order.updateToPaymentSuccess(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

            // user Cart 정리 outbox
            UserCartOutbox userCartOutbox = UserCartOutbox.create(order.getId(), order.getUserId());
            userCartOutboxEventRepository.save(userCartOutbox);
        }
        // 주문의 결제 실패 처리
        else order.updateToPaymentFailed();

        // 결제 처리 결과에 따른 재고 반영 outbox
        StockConfirmOutbox stockConfirmOutbox = StockConfirmOutbox.create(order.getId(), order.getUserId(), order.getStatus());
        stockConfirmOutboxRepository.save(stockConfirmOutbox);

        return new OrderUpdateResponseDto(true, false);
    }

    private OrderFetchResponseDto convertToOrderFetchResponse(Order order) {
        return new OrderFetchResponseDto(order.getUserId(), order.getId(), order.getAmount(), order.getStatus().name());
    }

    private Order getOrderWithDeliveryAndValidate(OrderUpdateRequestDto requestDto) {
        return orderRepository.findByIdAndUserIdWithDelivery(requestDto.getOrderId()).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. orderId = {}", requestDto.getOrderId());
            return new OrderException(ErrorCode.ORDER_NOT_FOUND, requestDto.getOrderId());
        });
    }


    // order 조회, 검증
    private Order fetchOrderAndValidate(String orderId, Long userId) {
         return orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. orderId = {}, userId = {}", orderId, userId);
            return new OrderException(ErrorCode.ORDER_NOT_FOUND, orderId, userId);
        });
    }
}
