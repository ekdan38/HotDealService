package com.hong.hotdeal.exception.handler;

import com.hong.hotdeal.exception.ErrorResponseDto;
import com.hong.hotdeal.exception.custom.EmailVerificationException;
import com.hong.hotdeal.exception.custom.MailSenderException;
import com.hong.hotdeal.exception.custom.SignupException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MailSenderException.class)
    public ResponseEntity<ErrorResponseDto> mailSenderException(MailSenderException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e.getErrorCode()));
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ErrorResponseDto> emailVerificationException(EmailVerificationException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e.getErrorCode()));
    }

    @ExceptionHandler(SignupException.class)
    public ResponseEntity<ErrorResponseDto> signupException(SignupException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e.getErrorCode()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> constraintViolationException(ConstraintViolationException e){
        return ResponseEntity.badRequest().body("이메일 형식을 따라야 합니다.");
    }

}
