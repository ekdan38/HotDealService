package com.hong.hotdeal.exception.custom;

import com.hong.hotdeal.exception.ErrorCode;

public class SignupException extends BusinessException {
    public SignupException(ErrorCode errorCode) {
        super(errorCode);
    }
}
