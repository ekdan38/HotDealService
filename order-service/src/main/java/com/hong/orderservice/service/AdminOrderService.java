package com.hong.orderservice.service;

import com.hong.orderservice.dto.OrderPagingResponseDto;
import com.hong.orderservice.dto.OrderResponseDto;

public interface AdminOrderService {

    // 주문 내역 페이징
    OrderPagingResponseDto getOrders(Long targetUserId, Long cursor, int size);

    // 주문 조회
    OrderResponseDto getOrder(Long targetUserId, Long orderId);
}
