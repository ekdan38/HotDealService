package com.hong.paymentservice.client.order;

import com.hong.common.dto.OrderFetchRequestDto;
import com.hong.common.dto.OrderFetchResponseDto;
import com.hong.common.dto.OrderUpdateRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @PostMapping("/order-service/orders")
    OrderFetchResponseDto fetchOrder(@RequestBody OrderFetchRequestDto requestDto);

    @PostMapping("/order-service/update")
    Boolean updateOrderStatus(@RequestBody OrderUpdateRequestDto requestDto);
}

