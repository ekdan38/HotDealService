package com.hong.paymentservice.web;

import com.hong.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[PaymentController]")
public class PaymentController {

    private final PaymentService paymentService;


}
