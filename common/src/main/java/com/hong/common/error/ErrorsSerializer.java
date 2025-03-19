package com.hong.common.error;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.validation.Errors;

import java.io.IOException;

@JsonComponent
public class ErrorsSerializer extends JsonSerializer<Errors> {
    @Override
    public void serialize(Errors errors, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 객체를 시작
        gen.writeStartObject();
        gen.writeStringField("message", "입력 값에 대한 검증을 실패했습니다.");

        // 에러를 배열로 작성
        gen.writeArrayFieldStart("errors");

        // 필드 관련 에러 직렬화
        errors.getFieldErrors().forEach(e -> {
            try {
                gen.writeStartObject(); // 객체 시작
                gen.writeStringField("field", e.getField());
                //gen.writeStringField("objectName", e.getObjectName());
                gen.writeStringField("code", e.getCode());
                gen.writeStringField("defaultMessage", e.getDefaultMessage());
                Object rejectedValue = e.getRejectedValue();
                if (rejectedValue != null) {
                    gen.writeStringField("rejectedValue", rejectedValue.toString());
                }
                gen.writeEndObject(); // 객체 종료
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // 글로벌 에러 직렬화
        errors.getGlobalErrors().forEach(e -> {
            try {
                gen.writeStartObject(); // 객체 시작
                gen.writeStringField("objectName", e.getObjectName());
                gen.writeStringField("code", e.getCode());
                gen.writeStringField("defaultMessage", e.getDefaultMessage());
                gen.writeEndObject(); // 객체 종료
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // 배열 종료
        gen.writeEndArray();

        // 객체 종료
        gen.writeEndObject();
    }
}