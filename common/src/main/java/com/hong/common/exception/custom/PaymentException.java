package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class PaymentException extends BusinessException{
    public PaymentException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
