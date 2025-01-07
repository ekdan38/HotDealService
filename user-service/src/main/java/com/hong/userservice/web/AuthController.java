package com.hong.userservice.web;

import com.hong.common.dto.ResponseDto;
import com.hong.common.dto.UserDto;
import com.hong.userservice.service.UserService;
import com.hong.userservice.dto.SignupResponseDto;
import com.hong.userservice.web.dto.EmailVerificationRequestDto;
import com.hong.userservice.web.dto.EmailVerifyRequestDto;
import com.hong.userservice.web.dto.SignupRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[AuthController]")
public class AuthController {

    private final UserService userService;


    // email 인증 요청
    @PostMapping("/email-verification")
    public ResponseEntity<?> emailVerification(@RequestBody @Validated EmailVerificationRequestDto requestDto,
                                               BindingResult bindingResult) {
        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("이메일 인증 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        userService.emailVerification(requestDto.getEmail());

        // 응답 설정
        ResponseDto<String> responseDto = new ResponseDto<>("인증 코드 전송 완료");
        return ResponseEntity.ok().body(responseDto);
    }

    // email 인증 검사
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody @Validated EmailVerifyRequestDto requestDto,
                                         BindingResult bindingResult) {
        // code 받고 검사
        if (bindingResult.hasErrors()) {
            log.error("이메일 인증 검사 요청 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        String result = userService.verifyCode(requestDto.getEmail(), requestDto.getCode());

        // 응답 설정
        ResponseDto<String> responseDto = new ResponseDto<>(result);
        return ResponseEntity.ok().body(responseDto);
    }

    // singup 처리
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Validated SignupRequestDto requestDto,
                                    BindingResult bindingResult) {

        // signupRequestDto 필드 에러
        if (bindingResult.hasErrors()) {
            log.error("회원 가입 요청 검사 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        SignupResponseDto result = userService.signup(requestDto);

        // 응답 설정
        ResponseDto<SignupResponseDto> responseDto = new ResponseDto<>("회원 가입 완료", result);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }


    // 토큰 재발급
    @PostMapping("/reissue")
    private ResponseEntity<ResponseDto<?>> reissue(HttpServletRequest request, HttpServletResponse response) {

        String result = userService.reissueToken(request, response);
        ResponseDto<?> responseDto = new ResponseDto<>(result);
        return ResponseEntity.ok().body(responseDto);
    }
}
