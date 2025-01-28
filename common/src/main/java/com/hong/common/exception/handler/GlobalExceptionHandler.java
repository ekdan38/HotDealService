package com.hong.common.exception.handler;

import com.hong.common.exception.ErrorResponseDto;
import com.hong.common.exception.custom.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> businessException(BusinessException e){
        log.error("Error Code: {}, Error Message: {}", e.getErrorCode().getErrorCode(), e.getMessage(), e);
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponseDto.of(e));
    }

}
