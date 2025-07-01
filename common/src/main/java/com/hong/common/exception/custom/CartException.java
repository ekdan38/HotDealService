package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class CartException extends BusinessException{
    public CartException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
