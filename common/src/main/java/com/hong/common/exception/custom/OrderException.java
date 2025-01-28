package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class OrderException extends BusinessException {
    public OrderException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
