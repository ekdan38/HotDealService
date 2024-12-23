package com.hong.hotdeal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.hotdeal.jwt.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration, JwtUtil jwtUtil,
                          CustomUserDetailsService customUserDetailsService, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)// csrf 비활성화
                .formLogin(AbstractHttpConfigurer::disable)// forLogin 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)// Basic 비활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/images/**", "/js/**", "/favicon.*", "/*/icon-*").permitAll()
                        .requestMatchers("/signup", "/email-verification", "/verify-email", "/login", "/reissue").permitAll()
                        // products 추후 권한 설정
                        .requestMatchers("/products/**").permitAll()
                        .anyRequest().authenticated())

                // UsernamePasswordAuthenticationFilter 위치에다가 LoginFilter을 넣어준다.
                .addFilterBefore(new JwtFilter(jwtUtil, customUserDetailsService, objectMapper), LoginFilter.class)
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil, redisTemplate, objectMapper), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, redisTemplate, objectMapper), LogoutFilter.class)

                // entryPoint, AccessDenied
                .exceptionHandling(except -> except
                        .accessDeniedHandler(new CustomAuthenticationDeniedHandler(objectMapper))
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint(objectMapper)))

                 // session 정책
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                );
        return http.build();
    }
}
