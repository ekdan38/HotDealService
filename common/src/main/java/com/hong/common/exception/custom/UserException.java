package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class UserException extends BusinessException{

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
