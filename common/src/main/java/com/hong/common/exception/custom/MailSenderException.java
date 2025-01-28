package com.hong.common.exception.custom;

import com.hong.common.exception.ErrorCode;

public class MailSenderException extends BusinessException {
    public MailSenderException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
