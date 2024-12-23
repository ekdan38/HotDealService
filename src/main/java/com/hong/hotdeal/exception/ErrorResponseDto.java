package com.hong.hotdeal.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponseDto {

    private String errorMessage;
    private String errorCode;

    public static ErrorResponseDto of(ErrorCode code){
        return new ErrorResponseDto(code.getErrorMessage(), code.getErrorCode());
    }
}
