package com.hong.hotdeal.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.hotdeal.web.dto.response.ResponseDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j(topic = "[CustomAuthenticationDeniedHandler]")
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // 권한 예외 처리
        log.error("권한 거부됨 = {}", accessDeniedException.getMessage());

        // 응답 설정
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("권한 없는 요청 경로", request.getRequestURI());
        ResponseDto responseDto = new ResponseDto<Map<String, Object>>("권한이 없는 요청입니다.", data);
        response.getWriter().write(objectMapper.writeValueAsString(responseDto));
    }
}
