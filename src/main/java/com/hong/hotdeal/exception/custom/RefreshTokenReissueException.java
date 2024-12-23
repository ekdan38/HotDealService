package com.hong.hotdeal.exception.custom;

import com.hong.hotdeal.exception.ErrorCode;

public class RefreshTokenReissueException extends BusinessException{
    public RefreshTokenReissueException(ErrorCode errorCode) {
        super(errorCode);
    }
}
