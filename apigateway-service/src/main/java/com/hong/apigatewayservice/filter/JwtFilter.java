package com.hong.apigatewayservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.ResponseDto;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j(topic = "[JwtFilter]")
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {
    private final Environment env;
    private final ObjectMapper objectMapper;
    private SecretKey secretKey;

    @Data
    public static class Config {
        private String requiredRole;
    }

    public JwtFilter(ObjectMapper objectMapper, Environment env) {
        super(Config.class);
        this.objectMapper = objectMapper;
        this.env = env;
    }

    @Override
    public GatewayFilter apply(Config config) {
        secretKey = new SecretKeySpec(
                env.getProperty("jwt.secret.key").getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.error("Authorization 헤더가 없습니다.");
                return setResponse(response, "Authorization 헤더가 없습니다.", null, HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.error("잘못된 형식의 AccessToken 입니다. = {}", authHeader);
                return setResponse(response, "잘못된 형식의 AccessToken 입니다.", authHeader, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.split(" ")[1];
            log.info("Extracted token: {}", token);

            return validateAccessToken(token, response)
                    .flatMap(isValid -> {
                        if (!isValid) {
                            return response.setComplete();
                        }

                        String role = getRole(token);
                        log.info("Token role: {}", role);

                        if (!hasRequiredRole(config.requiredRole, role)) {
                            log.error("접근 권한이 없습니다. 필요 권한: {}, 사용자 권한: {}", config.requiredRole, role);
                            return setResponse(response, "접근 권한이 없습니다.",
                                    "필요 권한 : " + config.getRequiredRole() + " 사용자 권한 : " + role,
                                    HttpStatus.FORBIDDEN);
                        }

                        String username = getUsername(token);
                        String userId = String.valueOf(getUserId(token));

                        log.info("Token details - userId: {}, username: {}, role: {}", userId, username, role);

                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header("X-User-Id", userId)
                                .header("X-User-Role", role)
                                .build();

                        log.info("Sending request with token: {}", token);

                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    });
        };
    }

    // accessToken 검증을 위한 메서드 (로컬 변수 token을 사용)
    private Mono<Boolean> validateAccessToken(String token, ServerHttpResponse response) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
        } catch (MalformedJwtException | SecurityException e) {
            log.error("유효하지 않는 JWT 서명 입니다. = {}", token);
            return setResponse(response, "유효하지 않는 JWT 서명 입니다.", token, HttpStatus.UNAUTHORIZED)
                    .thenReturn(false);
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT token 입니다. = {}", token);
            return setResponse(response, "만료된 AccessToken 입니다.", token, HttpStatus.UNAUTHORIZED)
                    .thenReturn(false);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰 입니다. = {}", token);
            return setResponse(response, "지원되지 않는 JWT 토큰 입니다.", token, HttpStatus.UNAUTHORIZED)
                    .thenReturn(false);
        }

        // 토큰의 category 검증 (발급 시 payload에 명시되어 있어야 함)
        String category = getCategory(token);
        if (!"access".equals(category)) {
            log.error("AccessToken 이 아닙니다. = {}", token);
            return setResponse(response, "AccessToken 이 아닙니다.", token, HttpStatus.UNAUTHORIZED)
                    .thenReturn(false);
        }
        return Mono.just(true);
    }

    private Long getUserId(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
                .getPayload().get("userId", Long.class);
    }

    private String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
                .getPayload().get("username", String.class);
    }

    private String getCategory(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
                .getPayload().get("category", String.class);
    }

    private String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
                .getPayload().get("role", String.class);
    }

    // 유저 권한 검증: requiredRole와 userRole이 일치하는지
    private boolean hasRequiredRole(String requiredRole, String userRole) {
        if ("USER".equals(requiredRole) && "ADMIN".equals(userRole)) {
            return true;
        }
        return requiredRole.equals(userRole);
    }

    // 응답 설정 메서드: ResponseDto를 JSON으로 직렬화하여 반환
    private Mono<Void> setResponse(ServerHttpResponse response, String message, String data, HttpStatusCode httpStatusCode) {
        response.setStatusCode(httpStatusCode);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        ResponseDto responseDto = new ResponseDto<>(message, data);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(responseDto);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            log.error("응답 생성 중 오류 발생: {}", e.getMessage());
            byte[] errorBytes = "{\"message\":\"서버 오류가 발생했습니다.\"}".getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBytes)));
        }
    }
}
