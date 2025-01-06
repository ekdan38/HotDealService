package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class CategoryException extends BusinessException{

    public CategoryException(ErrorCode errorCode) {
        super(errorCode);
    }
}
