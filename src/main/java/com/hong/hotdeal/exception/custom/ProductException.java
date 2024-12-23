package com.hong.hotdeal.exception.custom;

import com.hong.hotdeal.exception.ErrorCode;

public class ProductException extends BusinessException{

    public ProductException(ErrorCode errorCode) {
        super(errorCode);
    }
}
