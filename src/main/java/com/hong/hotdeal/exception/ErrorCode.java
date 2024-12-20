package com.hong.hotdeal.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    EMAIL_SENDER_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_00", "이메일 인증 코드 발송을 실패했습니다."),

    EMAIL_VERIFICATION_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "EMAIL_CODE_00", "인증 코드가 존재하지 않습니다."),
    EMAIL_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "EMAIL_CODE_01", "인증 코드가 일치하지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "EMAIL_CODE_02", "이메일 인증을 받지 않았습니다."),
    EMAIL_VERIFICATION_STATUS_INVALIED(HttpStatus.BAD_REQUEST, "EMAIL_CODE_03", "이메일 인증을 받지 않았습니다."),
    EMAIL_VERIFICATION_STATUS_NOT_FOUND(HttpStatus.NOT_FOUND, "EMAIL_CODE_04", "이메일 인증 요청을 먼저 보내세요."),
    EMAIL_VERIFICATION_STATUS_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "EMAIL_CODE_05", "이미 인증되었습니다."),
    SIGNUP_ENCRYPT_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "SIGNUP_00", "개인정보 암호화 처리중 오류가 발생했습니다."),
    SIGNUP_EXISTS_USERNAME(HttpStatus.BAD_REQUEST, "SIGNUP_01", "이미 존재하는 Username 입니다."),
    SIGNUP_EXISTS_EMAIL(HttpStatus.BAD_REQUEST, "SIGNUP_02", "이미 존재하는 Email 입니다.")
    ;
    private final HttpStatus status;
    private final String errorCode;
    private final String errorMessage;

    ErrorCode(HttpStatus status, String errorCode, String errorMessage) {
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
