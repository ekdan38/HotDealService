package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class ProductException extends BusinessException{

    public ProductException(ErrorCode errorCode) {
        super(errorCode);
    }
}
