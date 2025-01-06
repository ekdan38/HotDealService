package com.hong.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ResponseDto<T> {

    private String message;
    private T data;

    public ResponseDto(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public ResponseDto(String message) {
        this.message = message;
    }
}
