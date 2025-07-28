package com.hong.common.exception.custom;

public class PaymentAlreadyProcessedException extends RuntimeException{
    public PaymentAlreadyProcessedException(String message){
        super(message);
    }
}
