package com.hong.hotdeal.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 이메일 인증
    EMAIL_SENDER_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_CODE_00", "이메일 인증 코드 발송을 실패했습니다."),
    EMAIL_VERIFICATION_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "EMAIL_CODE_01", "인증 코드가 존재하지 않습니다."),
    EMAIL_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "EMAIL_CODE_02", "인증 코드가 일치하지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "EMAIL_CODE_03", "이메일 인증을 받지 않았습니다."),
    EMAIL_VERIFICATION_STATUS_INVALID(HttpStatus.BAD_REQUEST, "EMAIL_CODE_04", "이메일 인증을 받지 않았습니다."),
    EMAIL_VERIFICATION_STATUS_NOT_FOUND(HttpStatus.NOT_FOUND, "EMAIL_CODE_05", "이메일 인증 요청을 먼저 보내세요."),
    EMAIL_VERIFICATION_STATUS_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "EMAIL_CODE_06", "이미 인증 되었습니다."),

    // 회원 가입
    SIGNUP_EXISTS_USERNAME(HttpStatus.BAD_REQUEST, "SIGNUP_01", "이미 존재하는 Username 입니다."),
    SIGNUP_EXISTS_EMAIL(HttpStatus.BAD_REQUEST, "SIGNUP_02", "이미 존재하는 Email 입니다."),

    // 개인 정보 암호화 && 복호화
    CRYPTO_ENCRYPT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CRYPTO_01", "개인정보 암호화 처리중 오류가 발생했습니다."),
    CRYPTO_DECRYPT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CRYPTO_02", "개인정보 복호화 처리중 오류가 발생했습니다."),

    // RefreshToken
    REFRESH_TOKEN_NULL(HttpStatus.BAD_REQUEST, "REFRESH_TOKEN_00", "RefreshToken 이 null 입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "REFRESH_TOKEN_01", "만료된 RefreshToken 입니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "REFRESH_TOKEN_02", "RefreshToken 이 아닙니다."),

    // 상품
    PRODUCT_EXISTS(HttpStatus.BAD_REQUEST, "PRODUCT_00", "이미 존재하는 상품 입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.BAD_REQUEST, "PRODUCT_01", "존재하지 않는 상품 입니다."),
    PRODUCT_EXISTS_CATEGORY(HttpStatus.BAD_REQUEST, "PRODUCT_02", "상품에 카테고리가 이미 설정되어 있습니다."),

    // 카테고리
    CATEGORY_EXISTS(HttpStatus.BAD_REQUEST, "CATEGORY_00", "존재하지 않는 카테고리 입니다.")


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
