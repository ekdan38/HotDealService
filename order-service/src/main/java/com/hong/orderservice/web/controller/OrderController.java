package com.hong.orderservice.web.controller;

import com.hong.common.dto.ResponseDto;
import com.hong.orderservice.dto.OrderPagingResponseDto;
import com.hong.orderservice.dto.OrderResponseDto;
import com.hong.orderservice.service.OrderService;
import com.hong.orderservice.web.dto.OrderRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[OrderController]")
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    // 주문 생성
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestHeader("X-User-Id") Long userId,
                                         @RequestBody @Validated OrderRequestDto requestDto,
                                         BindingResult bindingResult) {

        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("주문 생성 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        OrderResponseDto resultDto = orderService.createOrder(userId, requestDto);

        // 응답 설정
        ResponseDto<OrderResponseDto> responseDto = new ResponseDto<>("주문 생성 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // 주문 내역 페이징
    @GetMapping
    public ResponseEntity<ResponseDto<OrderPagingResponseDto>> getOrders(@RequestHeader("X-User-Id") Long userId,
                                                                         @RequestParam(required = false) Long cursor,
                                                                         @RequestParam(required = false, defaultValue = "10") int size) {

        OrderPagingResponseDto resultDto = orderService.getOrders(userId, cursor, size);

        // 응답 설정
        ResponseDto<OrderPagingResponseDto> responseDto = new ResponseDto<>("주문 조회 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // 주문 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<ResponseDto<OrderResponseDto>> getOrder(@RequestHeader("X-User-Id") Long userId,
                                                                  @PathVariable("orderId") Long orderId) {
        OrderResponseDto resultDto = orderService.getOrder(userId, orderId);

        // 응답 설정
        ResponseDto<OrderResponseDto> responseDto = new ResponseDto<>("주문 조회 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // 주문 취소
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ResponseDto<OrderResponseDto>> cancelOrder(@RequestHeader("X-User-Id") Long userId,
                                         @PathVariable("orderId") Long orderId){

        OrderResponseDto resultDto = orderService.cancelOrder(userId, orderId);

        // 응답 설정
        ResponseDto<OrderResponseDto> responseDto = new ResponseDto<>("주문 취소 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // 반품
    @PatchMapping("{orderId}/return")
    public ResponseEntity<ResponseDto<OrderResponseDto>> returnOrder(@RequestHeader("X-User-Id") Long userId,
                                                                     @PathVariable("orderId") Long orderId){

        OrderResponseDto resultDto = orderService.returnOrder(userId, orderId);

        // 응답 설정
        ResponseDto<OrderResponseDto> responseDto = new ResponseDto<>("반품 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }


}
