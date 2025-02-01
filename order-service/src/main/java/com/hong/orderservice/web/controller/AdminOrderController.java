package com.hong.orderservice.web.controller;

import com.hong.common.dto.ResponseDto;
import com.hong.orderservice.dto.OrderPagingResponseDto;
import com.hong.orderservice.dto.OrderResponseDto;
import com.hong.orderservice.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[AdminOrderController]")
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<ResponseDto<OrderPagingResponseDto>> getOrders(@RequestParam(required = false) Long targetUserId,
                                                                         @RequestParam(required = false) Long cursor,
                                                                         @RequestParam(required = false, defaultValue = "10") int size) {

        OrderPagingResponseDto resultDto = adminOrderService.getOrders(targetUserId, cursor, size);

        // 응답 설정
        ResponseDto<OrderPagingResponseDto> responseDto = new ResponseDto<>("주문 조회 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    @GetMapping("/{orderId}/users/{userId}")
    public ResponseEntity<ResponseDto<OrderResponseDto>> getOrder(@PathVariable("userId") Long targetUserId,
                                                                  @PathVariable("orderId") Long orderId) {

        OrderResponseDto resultDto = adminOrderService.getOrder(targetUserId, orderId);
        // 응답 설정
        ResponseDto<OrderResponseDto> responseDto = new ResponseDto<>("주문 조회 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }
}
