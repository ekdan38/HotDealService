package com.hong.hotdeal.exception;

import com.hong.hotdeal.exception.custom.BusinessException;

public class OrderException extends BusinessException {
    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }
}
