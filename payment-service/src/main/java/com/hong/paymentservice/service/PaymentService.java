package com.hong.paymentservice.service;

import com.hong.paymentservice.dto.PaymentEntryResponseDto;
import com.hong.paymentservice.dto.PaymentProcessResponseDto;

public interface PaymentService {

    // 결제 진입
    PaymentEntryResponseDto paymentEntry(Long userId, Long orderId);

    // 결제 수행
    PaymentProcessResponseDto paymentProcess(Long userId, Long paymentId);
}
