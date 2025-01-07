package com.hong.userservice.service;

import com.hong.common.dto.UserDto;
import com.hong.userservice.dto.SignupResponseDto;
import com.hong.userservice.web.dto.SignupRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UserService {
    // 이메일 인증 요청 (인증 코드 발송)
    public void emailVerification(String email);

    // 인증 코드 확인
    public String verifyCode(String email, String requestCode);

    // 회원 가입
    public SignupResponseDto signup(SignupRequestDto requestDto);

    // 토큰 재발급
    public String reissueToken(HttpServletRequest request, HttpServletResponse response);

}
