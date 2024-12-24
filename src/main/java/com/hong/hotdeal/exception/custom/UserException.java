package com.hong.hotdeal.exception.custom;

import com.hong.hotdeal.exception.ErrorCode;

public class UserException extends BusinessException{

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
