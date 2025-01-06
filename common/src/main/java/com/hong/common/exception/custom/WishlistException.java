package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class WishlistException extends BusinessException{

    public WishlistException(ErrorCode errorCode) {
        super(errorCode);
    }
}
