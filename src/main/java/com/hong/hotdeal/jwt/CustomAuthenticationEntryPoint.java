package com.hong.hotdeal.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.hotdeal.web.dto.response.ResponseDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j(topic = "[CustomAuthenticationEntryPoint]")
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.error("인증 안된 사용자 요청 = {}", authException.getMessage());

        // 응답 설정
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("인증이 필요한 요청 경로", request.getRequestURI());
        ResponseDto responseDto = new ResponseDto<Map<String, Object>>("인증이 필요한 요청입니다.", data);
        response.getWriter().write(objectMapper.writeValueAsString(responseDto));
    }
}
