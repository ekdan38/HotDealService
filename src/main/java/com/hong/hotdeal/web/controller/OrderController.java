package com.hong.hotdeal.web.controller;

import com.hong.hotdeal.jwt.CustomUserDetails;
import com.hong.hotdeal.service.OrderService;
import com.hong.hotdeal.web.dto.request.order.OrderRequestDto;
import com.hong.hotdeal.web.dto.response.ResponseDto;
import com.hong.hotdeal.web.dto.response.order.OrderPagingResponseDto;
import com.hong.hotdeal.web.dto.response.order.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    @PostMapping
    public ResponseEntity<ResponseDto<OrderResponseDto>> createOrder(@RequestBody OrderRequestDto requestDto,
                                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {

        OrderResponseDto result = orderService.createOrder(userDetails.getUser().getId(), requestDto);

        // 응답 설정
        ResponseDto<OrderResponseDto> responseDto = new ResponseDto<>("주문 성공.", result);
        return ResponseEntity.ok().body(responseDto);
    }

    // 주문 페이징 조회
    @GetMapping
    public ResponseEntity<ResponseDto<OrderPagingResponseDto>> getOrders(@RequestParam(value = "cursor", required = false) Long cursor,
                                                                         @RequestParam(value = "size", defaultValue = "10", required = false) int size,
                                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (cursor == null) cursor = Long.MAX_VALUE;

        OrderPagingResponseDto result = orderService.getOrders(userDetails.getUser().getId(), cursor, size);
        // 응답 설정
        ResponseDto<OrderPagingResponseDto> responseDto = new ResponseDto<>("조회 성공", result);
        return ResponseEntity.ok().body(responseDto);
    }

    // 주문 단건 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<ResponseDto<OrderResponseDto>> getOrder(@PathVariable("orderId") Long orderId,
                                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {

        OrderResponseDto result = orderService.getOrder(userDetails.getUser().getId(), orderId);
        // 응답 설정
        ResponseDto<OrderResponseDto> responseDto = new ResponseDto<>("조회 성공", result);
        return ResponseEntity.ok().body(responseDto);
    }

    @DeleteMapping("{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable("orderId") Long orderId,
                                         @AuthenticationPrincipal CustomUserDetails userDetails){

        orderService.cancelOrder(userDetails.getUser().getId(), orderId);

        // 응답 설정
        ResponseDto<?> responseDto = new ResponseDto<>("배송 취소 성공.");
        return ResponseEntity.ok().body(responseDto);
    }


}
