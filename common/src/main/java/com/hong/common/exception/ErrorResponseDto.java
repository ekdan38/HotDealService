package com.hong.common.exception;

import com.hong.common.exception.custom.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponseDto {

    private String errorCode;
    private String errorMessage;

    public static ErrorResponseDto of(BusinessException e){
        return new ErrorResponseDto(e.getErrorCode().getErrorCode(), e.getMessage());
    }
}
