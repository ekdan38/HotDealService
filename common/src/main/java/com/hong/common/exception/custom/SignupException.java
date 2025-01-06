package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class SignupException extends BusinessException {
    public SignupException(ErrorCode errorCode) {
        super(errorCode);
    }
}
