package com.hong.hotdeal.exception.custom;

import com.hong.hotdeal.exception.ErrorCode;

public class EmailVerificationException extends BusinessException {

    public EmailVerificationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
