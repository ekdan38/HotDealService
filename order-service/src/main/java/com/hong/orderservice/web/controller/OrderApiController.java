package com.hong.orderservice.web.controller;

import com.hong.common.dto.OrderFetchRequestDto;
import com.hong.common.dto.OrderFetchResponseDto;
import com.hong.common.dto.OrderUpdateRequestDto;
import com.hong.common.dto.OrderUpdateResponseDto;
import com.hong.orderservice.service.OrderApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[OrderApiController]")
@RequestMapping("/order-service/orders")
public class OrderApiController {

    private final OrderApiService orderApiService;

    @PostMapping("/fetch")
    public ResponseEntity<OrderFetchResponseDto> fetchOrder(@RequestBody OrderFetchRequestDto requestDto){
        OrderFetchResponseDto resultDto = orderApiService.fetchOrder(requestDto);

        return ResponseEntity.ok().body(resultDto);
    }

    @PostMapping("/update")
    public ResponseEntity<OrderUpdateResponseDto> updateOrderStatus(@RequestBody OrderUpdateRequestDto requestDto){
        OrderUpdateResponseDto resultDto = orderApiService.updateOrderAndDelivery(requestDto);
        return ResponseEntity.ok().body(resultDto);
    }
}
