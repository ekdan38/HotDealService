package com.hong.orderservice.web.controller;

import com.hong.common.dto.OrderFetchRequestDto;
import com.hong.common.dto.OrderFetchResponseDto;
import com.hong.orderservice.service.OrderApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[OrderApiController]")
@RequestMapping("/order-service/orders")
public class OrderApiController {

    private final OrderApiService orderApiService;


    @PostMapping
    public ResponseEntity<OrderFetchResponseDto> fetchOrder(@RequestBody OrderFetchRequestDto requestDto){

        OrderFetchResponseDto resultDto = orderApiService.fetchOrder(requestDto);
        // 응답 설정
        return ResponseEntity.ok().body(resultDto);
    }
}
