package com.hong.hotdeal.exception.custom;

import com.hong.hotdeal.exception.ErrorCode;

public class CategoryException extends BusinessException{

    public CategoryException(ErrorCode errorCode) {
        super(errorCode);
    }
}
