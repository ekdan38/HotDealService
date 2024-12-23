package com.hong.hotdeal.web.controller.auth;

import com.hong.hotdeal.web.dto.response.ResponseDto;
import com.hong.hotdeal.service.SignupService;
import com.hong.hotdeal.web.dto.request.signup.EmailVerificationRequestDto;
import com.hong.hotdeal.web.dto.request.signup.EmailVerifyRequestDto;
import com.hong.hotdeal.web.dto.request.signup.SignupRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    // email 인증 요청
    @PostMapping("/email-verification")
    public ResponseEntity<?> emailVerification(@RequestBody @Validated EmailVerificationRequestDto requestDto,
                                               BindingResult bindingResult){
        // 요청 dto 오류 검사
        if(bindingResult.hasErrors()){
            log.error("Signup Validation Error = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        signupService.emailVerification(requestDto.getEmail());

        // 응답 설정
        ResponseDto<String> responseDto = new ResponseDto<>("인증 코드 전송 완료");
        return ResponseEntity.ok().body(responseDto);
    }

    // email 인증 검사
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody @Validated EmailVerifyRequestDto requestDto,
                                         BindingResult bindingResult){
        // code 받고 검사
        if(bindingResult.hasErrors()){
            log.error("Signup Validation Error = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        String result = signupService.verifyCode(requestDto.getEmail(), requestDto.getCode());

        // 응답 설정
        ResponseDto<String> responseDto = new ResponseDto<>(result);
        return ResponseEntity.ok().body(responseDto);
    }

    // singup 처리
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Validated SignupRequestDto requestDto,
                                 BindingResult bindingResult){

        // signupRequestDto 필드 에러
        if(bindingResult.hasErrors()){
            log.error("Signup Validation Error = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        Long userId = signupService.signup(requestDto);

        // 응답 설정
        HashMap<String, Long> data = new HashMap<>();
        data.put("id", userId);
        ResponseDto<HashMap<String, Long>> responseDto = new ResponseDto<>("회원 가입 완료", data);
        return ResponseEntity.ok().body(responseDto);
    }

}
