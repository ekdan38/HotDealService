package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class PaymentSessionException extends BusinessException{
    public PaymentSessionException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
