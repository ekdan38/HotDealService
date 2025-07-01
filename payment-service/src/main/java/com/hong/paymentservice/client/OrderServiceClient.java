package com.hong.paymentservice.client;

import com.hong.common.dto.OrderFetchRequestDto;
import com.hong.common.dto.OrderFetchResponseDto;
import com.hong.common.dto.OrderUpdateRequestDto;
import com.hong.common.dto.OrderUpdateResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @PostMapping("/order-service/orders/fetch")
    OrderFetchResponseDto fetchOrder(@RequestBody OrderFetchRequestDto requestDto);

    @PostMapping("/order-service/orders/update")
    OrderUpdateResponseDto updateOrderStatus(@RequestBody OrderUpdateRequestDto requestDto);
}