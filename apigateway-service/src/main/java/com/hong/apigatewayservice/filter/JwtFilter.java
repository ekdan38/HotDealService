package com.hong.apigatewayservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.apigatewayservice.client.UserServiceClient;
import com.hong.common.dto.ResponseDto;
import io.jsonwebtoken.*;
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
    private final UserServiceClient userServiceClient;
    private SecretKey secretKey;
    private String accessToken;

    @Data
    public static class Config {
        private String requiredRole;
    }

    public JwtFilter(ObjectMapper objectMapper, Environment env, UserServiceClient userServiceClient) {
        super(Config.class);
        this.objectMapper = objectMapper;
        this.env = env;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public GatewayFilter apply(Config config) {
        secretKey = new SecretKeySpec(env.getProperty("jwt.secret.key")
                .getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build()
                        .getAlgorithm());

        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // Header에 Authorization 헤더가 없으면
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.error("Authorization 헤더가 없습니다.");
                // 응답 설정
                return setResponse(response, "Authorization 헤더가 없습니다.", null, HttpStatus.UNAUTHORIZED);
            }

            // accessToken 검증
            return validateAccessToken(request, response)
                    .flatMap(isValid -> {
                        if (!isValid) {
                            return response.setComplete();
                        }

                        // role 검증
                        String role = getRole(accessToken);
                        if (!hasRequiredRole(config.requiredRole, role)) {
                            log.error("접근 권한이 없습니다. 필요 권한: {}, 사용자 권한: {}", config.requiredRole, role);
                            // 응답 설정
                            return setResponse(response, "접근 권한이 없습니다.",
                                    "필요 권한 : " + config.getRequiredRole() + "사용자 권한 : " + role,
                                    HttpStatus.FORBIDDEN);
                        }
                        String username = getUsername(accessToken);
                        String userId = String.valueOf(getUserId(accessToken));

                        // 비동기로 유저 검증
                        return userServiceClient.validateUser(username)
                                .flatMap(isValidUser -> {
                                    if (!isValidUser) {
                                        log.error("username과 일치하는 User가 없습니다. {}", username);
                                        return setResponse(response, "username과 일치하는 User가 없습니다.", username, HttpStatus.UNAUTHORIZED);
                                    }

                                    // 유효한 User라면 요청 헤더에 정보 추가
                                    ServerHttpRequest modifiedRequest = request.mutate()
                                            .header("X-User-Id", String.valueOf(userId))
                                            .build();

                                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                                });
                    });
        });
    }

    // AccessToken 검증
    private Mono<Boolean> validateAccessToken(ServerHttpRequest request, ServerHttpResponse response) {
        accessToken = request.getHeaders().getFirst(org.springframework.http.HttpHeaders.AUTHORIZATION);

        if (!accessToken.startsWith("Bearer ")) {
            log.error("잘못된 형식의 AccessToken 입니다. = {}", accessToken);
            // 응답 설정
            return setResponse(response, "잘못된 형식의 AccessToken 입니다.", accessToken, HttpStatus.UNAUTHORIZED)
                    .thenReturn(false);
        }

        String[] split = accessToken.split(" ");
        if (split.length < 2) {
            log.error("잘못된 형식의 AccessToken 입니다. = {}", accessToken);
            // 응답 설정
            return setResponse(response, "잘못된 형식의 AccessToken 입니다.", accessToken, HttpStatus.UNAUTHORIZED)
                    .thenReturn(false);
        }
        accessToken = split[1];

        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(accessToken);
        } catch (MalformedJwtException | SecurityException e) {
            log.error("유효하지 않는 JWT 서명 입니다. = {}", accessToken);
            return setResponse(response, "유효하지 않는 JWT 서명 입니다.", accessToken, HttpStatus.UNAUTHORIZED)
                    .thenReturn(false);
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT token 입니다. = {}", accessToken);
            return setResponse(response, "만료된 AccessToken 입니다.", accessToken, HttpStatus.UNAUTHORIZED)
                    .thenReturn(false);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰 입니다. = {}", accessToken);
            return setResponse(response, "지원되지 않는 JWT 토큰 입니다. ", accessToken, HttpStatus.UNAUTHORIZED)
                    .thenReturn(false);
        }

        // 토큰이 access인지 확인 (발급시 페이로드에 명시)
        String category = getCategory(accessToken);

        if (!"access".equals(category)) {
            log.error("AccessToken 이 아닙니다. = {}", accessToken);
            // 응답 설정
            return setResponse(response, "AccessToken 이 아닙니다.", accessToken, HttpStatus.UNAUTHORIZED)
                    .thenReturn(false);
        }
        // 단일 ture를 포함하는 Mono 생성
        return Mono.just(true);
    }

    private Long getUserId(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("userId", Long.class);
    }

    private String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
    }

    private String getCategory(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("category", String.class);
    }

    private String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    // 유저 권한 검증
    private boolean hasRequiredRole(String requiredRole, String userRole) {
        if (requiredRole.equals("USER")) {
            if (userRole.equals("ADMIN")) {
                return true;
            }
        }
        return requiredRole.equals(userRole);
    }

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
