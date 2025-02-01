package com.hong.paymentservice.service;

import com.hong.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[PaymentServiceImpl]")
public class PaymentServiceImpl {

    private final PaymentRepository paymentRepository;
}
