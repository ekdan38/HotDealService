package com.hong.hotdeal.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.hotdeal.web.dto.response.ResponseDto;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@Slf4j(topic = "[CustomLogoutFilter]")
public class CustomLogoutFilter extends GenericFilterBean {

    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public CustomLogoutFilter(JwtUtil jwtUtil, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

        // path
        String requestUri = request.getRequestURI();
        if (!requestUri.matches("^\\/logout$")) {

            filterChain.doFilter(request, response);
            return;
        }
        String requestMethod = request.getMethod();
        if (!requestMethod.equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        // RefreshToken 가져오기
        String refresh = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refresh")) {
                refresh = cookie.getValue();
            }
        }

        // Refresh null 확인
        if (refresh == null) {
            log.error("RefreshToken 이 null = {}", refresh);
            setResponse(response, "RefreshToken 이 null 입니다.");
            return;
        }

        //expired 확인
        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {
            // 응답 설정
            log.error("만료된 RefreshToken = {}", refresh);
            setResponse(response, "만료된 RefreshToken 입니다.");
            return;
        }

        // 토큰이 refresh인지 확인 (발급시 페이로드에 명시)
        String category = jwtUtil.getCategory(refresh);
        if (!category.equals("refresh")) {
            // 응답 설정
            log.error("RefreshToken 이 아님 = {}", refresh);
            setResponse(response, "RefreshToken 이 아닙니다.");
            return;
        }

        // Redis 에 저장되어 있는지 확인
        String username = jwtUtil.getUsername(refresh);

        String savedRefreshToken = opsForValue.get(username + ":refresh");

        // 해당하는 값이 없으면
        if(savedRefreshToken == null){
            log.error("DB에 RefreshToken 이 존재하지 않음 = {}", refresh);
            setResponse(response, "DB에 RefreshToken 이 존재하지 않습니다.");
            return;
        }
        // 있으면
        // 로그아웃 진행
        // Refresh Token Redis에서 제거
        opsForValue.getOperations().delete(username + ":refresh");

        // Refresh 토큰 Cookie 값 0
        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");

        response.addCookie(cookie);
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ResponseDto responseDto = new ResponseDto<>("로그아웃 성공.");
        response.getWriter().write(objectMapper.writeValueAsString(responseDto));
    }
    private void setResponse(HttpServletResponse response, String data) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ResponseDto responseDto = new ResponseDto<>(data);
        response.getWriter().write(objectMapper.writeValueAsString(responseDto));
    }
}
