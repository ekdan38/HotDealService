package com.hong.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.userservice.jwt.CustomLogoutFilter;
import com.hong.userservice.jwt.JwtUtil;
import com.hong.userservice.jwt.LoginFilter;
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

    private final AuthenticationConfiguration authenticationConfiguration;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration,
                          StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper,
                          JwtUtil jwtUtil) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
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
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/signup", "/jwt","/email-verification", "/verify-email", "/login", "/reissue", "/health_check").permitAll()

                        .anyRequest().permitAll())

//                .anyRequest().authenticated())

//                // UsernamePasswordAuthenticationFilter 위치에다가 LoginFilter을 넣어준다.
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil, redisTemplate, objectMapper), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, redisTemplate, objectMapper), LogoutFilter.class)


                // session 정책
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                );
        return http.build();
    }
}