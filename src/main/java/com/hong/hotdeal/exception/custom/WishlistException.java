package com.hong.hotdeal.exception.custom;

import com.hong.hotdeal.exception.ErrorCode;

public class WishlistException extends BusinessException{

    public WishlistException(ErrorCode errorCode) {
        super(errorCode);
    }
}
