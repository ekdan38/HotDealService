package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class HotDealException extends BusinessException{
    public HotDealException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
