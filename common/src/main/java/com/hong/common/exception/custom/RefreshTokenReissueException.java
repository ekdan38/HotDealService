package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class RefreshTokenReissueException extends BusinessException{
    public RefreshTokenReissueException(ErrorCode errorCode, Object... args)
    {
        super(errorCode, args);
    }
}
