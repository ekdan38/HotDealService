package com.hong.paymentservice.web;

import com.hong.common.dto.ResponseDto;
import com.hong.paymentservice.dto.PaymentEntryResponseDto;
import com.hong.paymentservice.dto.PaymentProcessRequestDto;
import com.hong.paymentservice.dto.PaymentProcessResponseDto;
import com.hong.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[PaymentController]")
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/entry/{orderId}")
    public ResponseEntity<ResponseDto<PaymentEntryResponseDto>> paymentEntry(@RequestHeader("X-User-Id") Long userId,
                                                                             @PathVariable("orderId") Long orderId) {

        PaymentEntryResponseDto resultDto = paymentService.paymentEntry(userId, orderId);

        // 응답 설정
        ResponseDto<PaymentEntryResponseDto> responseDto = new ResponseDto<>("결제 진입 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    @PostMapping("/process/{paymentId}")
    public ResponseEntity<ResponseDto<PaymentProcessResponseDto>> paymentProcess(@RequestHeader("X-User-Id") Long userId,
                                                                                 @PathVariable("paymentId") Long paymentId,
                                                                                 @RequestBody PaymentProcessRequestDto requestDto) {

        PaymentProcessResponseDto resultDto = paymentService.paymentProcess(userId, paymentId, requestDto.getUserPaymentAmount());

        // 응답 설정
        ResponseDto<PaymentProcessResponseDto> responseDto = new ResponseDto<>("결제 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

}
