package com.hong.paymentservice.web;

import com.hong.common.dto.*;
import com.hong.common.dto.kafka.RefundOrderEventDto;
import com.hong.paymentservice.service.PaymentApiService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment-service")
public class PaymentApiController {

    private final PaymentApiService paymentApiService;

    // payment 생성
    @PostMapping("/payments")
    public ResponseEntity<PaymentCreateResponseDto> createPayment(@RequestBody PaymentCreateRequestDto requestDto){
        PaymentCreateResponseDto resultDto = paymentApiService.createPayment(requestDto);
        return ResponseEntity.status(HttpStatus.SC_CREATED).body(resultDto);
    }


    // 환불로 인한 payment 취소
    @PostMapping("/payments/cancel")
    public ResponseEntity<PaymentCancelResponseDto> cancelPayment(@RequestBody PaymentCancelRequestDto requestDto){
        PaymentCancelResponseDto resultDto = paymentApiService.updtaeToCanceled(new RefundOrderEventDto(requestDto.getOrderId(), requestDto.getUserId()));
        return ResponseEntity.ok().body(resultDto);
    }

}
