package com.hong.orderservice.service;

import com.hong.orderservice.dto.OrderPagingResponseDto;
import com.hong.orderservice.dto.OrderResponseDto;
import com.hong.orderservice.web.dto.OrderRequestDto;

public interface OrderService {

    // 주문 생성
    OrderResponseDto createOrder(Long userId, OrderRequestDto requestDto);

    // 주문 내역 페이징
    OrderPagingResponseDto getOrders(Long userId, String cursor, int size);

    // 주문 조회
    OrderResponseDto getOrder(Long userId, String orderId);

    // 주문 취소
    OrderResponseDto cancelOrder(Long userId, String orderId);

    // 반품
    OrderResponseDto refundOrder(Long userId, String orderId);
}
