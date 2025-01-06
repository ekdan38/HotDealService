package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class EmailVerificationException extends BusinessException {

    public EmailVerificationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
