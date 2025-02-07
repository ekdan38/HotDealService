package com.hong.paymentservice.service;

import org.springframework.stereotype.Service;

@Service
public class FakePaymentGateway {

    public boolean processPayment(int amount, int userPaymentAmount){
        if(userPaymentAmount < amount) return false;
        return true;
    }
}
