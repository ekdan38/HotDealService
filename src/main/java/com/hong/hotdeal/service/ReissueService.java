package com.hong.hotdeal.service;

import com.hong.hotdeal.exception.ErrorCode;
import com.hong.hotdeal.exception.custom.RefreshTokenReissueException;
import com.hong.hotdeal.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[ReissueService]")
public class ReissueService {
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public String reissueToken(HttpServletRequest request, HttpServletResponse response){
        // Refresh Token 추출
        String refresh = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refresh")) {
                refresh = cookie.getValue();
            }
        }

        if (refresh == null) {
            log.error("RefreshToken 이 null = {}", refresh);
            throw new RefreshTokenReissueException(ErrorCode.REFRESH_TOKEN_NULL);
        }

        // 만료 체크
        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {
            log.error("만료된 RefreshToken = {}", refresh);
            throw new RefreshTokenReissueException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // 토큰이 refresh인지 확인 (발급시 페이로드에 명시)
        String category = jwtUtil.getCategory(refresh);

        if (!category.equals("refresh")) {
            log.error("RefreshToken 이 아님 = {}", refresh);
            throw new RefreshTokenReissueException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String username = jwtUtil.getUsername(refresh);
        String role = jwtUtil.getRole(refresh);

        // Access, Refresh Token 재발급 => TokenRotation
        String newAccess = jwtUtil.createJwt("access", username, role, 600000L);
        String newRefresh = jwtUtil.createJwt("refresh", username, role, 86400000L);

        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        opsForValue.set(username + ":refresh", refresh, 24, TimeUnit.HOURS);

        // 응답 설정
        response.setHeader("Authorization", "Bearer " + newAccess);
        response.addCookie(createCookie("refresh", newRefresh));
        return "AccessToken, RefreshToken 재발급 완료.";
    }
    private Cookie createCookie(String key, String value){
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60); // 24시간
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}
