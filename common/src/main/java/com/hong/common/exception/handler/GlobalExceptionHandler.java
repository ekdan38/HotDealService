package com.hong.common.exception.handler;

import com.hong.common.exception.ErrorResponseDto;
import com.hong.common.exception.custom.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> businessException(BusinessException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e.getErrorCode()));
    }

}
