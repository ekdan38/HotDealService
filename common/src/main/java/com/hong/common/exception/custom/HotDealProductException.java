package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class HotDealProductException extends BusinessException{
    public HotDealProductException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
