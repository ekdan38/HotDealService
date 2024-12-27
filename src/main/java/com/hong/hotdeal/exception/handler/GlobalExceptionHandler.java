package com.hong.hotdeal.exception.handler;

import com.hong.hotdeal.exception.ErrorResponseDto;
import com.hong.hotdeal.exception.OrderException;
import com.hong.hotdeal.exception.custom.*;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> businessException(BusinessException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e.getErrorCode()));
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> constraintViolationException(ConstraintViolationException e){
        return ResponseEntity.badRequest().body("이메일 형식을 따라야 합니다.");
    }

}
