package com.hong.paymentservice.service;

import com.hong.common.dto.*;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.PaymentException;
import com.hong.paymentservice.domain.Payment;
import com.hong.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[PaymentApiService]")
public class PaymentApiService {

    private final PaymentRepository paymentRepository;

    // payment 생성
    @Transactional
    public PaymentCreateResponseDto createPayment(PaymentCreateRequestDto requestDto){
        String orderId = requestDto.getOrderId();
        // 1. 이미 생성된 결제인지 확인
        Optional<Payment> optionalPayment = paymentRepository.findByOrderId(orderId);
        if(optionalPayment.isPresent()){
            Payment payment = optionalPayment.get();
            log.warn("이미 생성된 결제입니다. orderId = {}", orderId);
            return new PaymentCreateResponseDto(payment.getId(), true);
        }
        // 2. 없다면 생성
        else{
            Payment payment = Payment.create(orderId, requestDto.getAmount(), requestDto.getUserId(), requestDto.getExpireAt());
            paymentRepository.save(payment);
            return new PaymentCreateResponseDto(payment.getId(), true);
        }
    }

    // payment 만료 처리
    @Transactional
    public ExpirePaymentResponseDto expirePayment(ExpirePaymentRequestDto requestDto){
        String orderId = requestDto.getOrderId();
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(() -> {
            log.error("존재 하지 않는 결제입니다. orderId = {}", orderId);
            return new PaymentException(ErrorCode.PAYMENT_NOT_FOUND, null, orderId);
        });
        payment.updateToExpired();
        log.info("payment Expired 처리 완료. orderId = {}", orderId);
        return new ExpirePaymentResponseDto(true, false);
    }

    // payment 취소 처리
    @Transactional
    public PaymentCancelResponseDto cancelPayment(PaymentCancelRequestDto requestDto) {
        String orderId = requestDto.getOrderId();
        Long userId = requestDto.getUserId();

        Payment payment = paymentRepository.findByOrderIdAndUserId(orderId, userId).orElseThrow(() -> {
            log.error("존재 하지 않는 결제입니다. userId = {}, orderId = {}", userId, orderId);
            return new PaymentException(ErrorCode.PAYMENT_NOT_FOUND, userId, orderId);
        });

        // 취소 처리
        payment.updateToCancel();

        return new PaymentCancelResponseDto(true, false);
    }
}
