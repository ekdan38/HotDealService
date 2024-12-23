package com.hong.hotdeal.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.hotdeal.domain.User;
import com.hong.hotdeal.web.dto.response.ResponseDto;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j(topic = "[JwtFilter]")
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    public JwtFilter(JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //request에서 Authorization 헤더를 찾음
        String accessToken= request.getHeader("Authorization");

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }
        // 토큰 값 추출
        String[] split = accessToken.split(" ");

        if (split.length < 2){
            log.error("AccessToken 형식이 잘못됨 = {}", accessToken);
            setResponse(response,  "잘못된 형식의 AccessToken 입니다.");
            return;
        }

        accessToken = split[1];

        // 토큰 만료 여부 확인, 만료시 다음 필터로 넘기지 않음
        try {
            jwtUtil.isExpired(accessToken);
        } catch (ExpiredJwtException e) {
            log.error("만료된 AccessToken = {}", accessToken);
            // 응답 설정
            setResponse(response, "만료된 AccessToken 입니다.");
            return;
        }

        // 토큰이 access인지 확인 (발급시 페이로드에 명시)
        String category = jwtUtil.getCategory(accessToken);

        if (!category.equals("access")) {
            log.error("AccessToken 이 아님 = {}", accessToken);
            // 응답 설정
            setResponse(response, "AccessToken 이 아닙니다.");
            return;
        }
        //토큰에서 username과 role 획득
        String username = jwtUtil.getUsername(accessToken);
        String role = jwtUtil.getRole(accessToken);

        CustomUserDetails customUserDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(username);
        User user = customUserDetails.getUser();

        if(!user.getRole().name().equals(role)){
            log.error("정상적이지 않은 AccessToken = {}", accessToken);
            setResponse(response, "정상적인 AccessToken 이 아닙니다.");
        }
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }
    private void setResponse(HttpServletResponse response, String data) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ResponseDto responseDto = new ResponseDto<>(data);
        response.getWriter().write(objectMapper.writeValueAsString(responseDto));
    }
}
