package com.hong.orderservice.client.payment;


import com.hong.common.dto.PaymentCancelRequestDto;
import com.hong.common.dto.PaymentCancelResponseDto;
import com.hong.common.dto.PaymentCreateRequestDto;
import com.hong.common.dto.PaymentCreateResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @PostMapping("/payment-service/payments")
    PaymentCreateResponseDto createPayment(@RequestBody PaymentCreateRequestDto requestDto);

    @PostMapping("/payment-service/payments/cancel")
    PaymentCancelResponseDto cancelPayment(@RequestBody PaymentCancelRequestDto requestDto);
}
