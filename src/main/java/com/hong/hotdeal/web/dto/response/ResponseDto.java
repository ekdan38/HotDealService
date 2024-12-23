package com.hong.hotdeal.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Null 값을 가진 필드는 직렬화에서 제외
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
