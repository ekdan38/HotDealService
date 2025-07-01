package com.hong.paymentservice.service;

import com.hong.paymentservice.dto.PaymentPerformResponseDto;
import com.hong.paymentservice.dto.PaymentPrepareResponseDto;
import com.hong.paymentservice.web.dto.PaymentPerformRequestDto;
import com.hong.paymentservice.web.dto.PaymentPrepareRequestDto;

public interface PaymentService {

    // 결제 진입
    PaymentPrepareResponseDto paymentPrepare(Long userId, PaymentPrepareRequestDto requestDto);

    // 결제 수행
    PaymentPerformResponseDto performPayment(Long userId, PaymentPerformRequestDto requestDto);
}
