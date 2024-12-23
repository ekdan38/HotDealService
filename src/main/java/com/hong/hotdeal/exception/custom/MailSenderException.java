package com.hong.hotdeal.exception.custom;

import com.hong.hotdeal.exception.ErrorCode;

public class MailSenderException extends BusinessException {
    public MailSenderException(ErrorCode errorCode) {
        super(errorCode);
    }
}
