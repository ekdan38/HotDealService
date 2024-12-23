package com.hong.hotdeal.exception.handler;

import com.hong.hotdeal.exception.ErrorResponseDto;
import com.hong.hotdeal.exception.custom.*;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 이메일 전송 예외
    @ExceptionHandler(MailSenderException.class)
    public ResponseEntity<ErrorResponseDto> mailSenderException(MailSenderException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e.getErrorCode()));
    }

    // 이메일 인증 코드 검사 예외
    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ErrorResponseDto> emailVerificationException(EmailVerificationException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e.getErrorCode()));
    }

    // 회원 가입 예외
    @ExceptionHandler(SignupException.class)
    public ResponseEntity<ErrorResponseDto> signupException(SignupException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e.getErrorCode()));
    }

    // RefreshToken 재발급 예외
    @ExceptionHandler(RefreshTokenReissueException.class)
    public ResponseEntity<?> refreshTokenReissueException(RefreshTokenReissueException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e.getErrorCode()));
    }

    // 상품 예외
    @ExceptionHandler(ProductException.class)
    public ResponseEntity<?> productException(ProductException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e.getErrorCode()));
    }

    // 카테고리 예외
    @ExceptionHandler(CategoryException.class)
    public ResponseEntity<?> categoryException(CategoryException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e.getErrorCode()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> constraintViolationException(ConstraintViolationException e){
        return ResponseEntity.badRequest().body("이메일 형식을 따라야 합니다.");
    }

}
