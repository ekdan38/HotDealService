package com.hong.paymentservice.web;

import com.hong.common.dto.ResponseDto;
import com.hong.paymentservice.dto.PaymentPerformResponseDto;
import com.hong.paymentservice.dto.PaymentPrepareResponseDto;
import com.hong.paymentservice.service.PaymentService;
import com.hong.paymentservice.web.dto.PaymentPerformRequestDto;
import com.hong.paymentservice.web.dto.PaymentPrepareRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[PaymentController]")
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/prepare")
    public ResponseEntity<?> paymentEntry(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody @Validated PaymentPrepareRequestDto requestDto,
                                          BindingResult bindingResult) {

        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("결제 진입 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        PaymentPrepareResponseDto resultDto = paymentService.paymentPrepare(userId, requestDto);

        // 응답 설정
        ResponseDto<PaymentPrepareResponseDto> responseDto = new ResponseDto<>("결제 진입 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    @PostMapping("/perform")
    public ResponseEntity<ResponseDto<PaymentPerformResponseDto>> performPayment(@RequestHeader("X-User-Id") Long userId,
                                                                                 @RequestBody @Validated PaymentPerformRequestDto request) {
        PaymentPerformResponseDto resultDto = paymentService.performPayment(userId, request);

        ResponseDto<PaymentPerformResponseDto> responseDto = new ResponseDto<>("결제 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

}
