package com.hong.hotdeal.service;

import com.hong.hotdeal.domain.Address;
import com.hong.hotdeal.domain.User;
import com.hong.hotdeal.domain.status.Role;
import com.hong.hotdeal.repository.UserRepository;
import com.hong.hotdeal.exception.ErrorCode;
import com.hong.hotdeal.exception.custom.EmailVerificationException;
import com.hong.hotdeal.exception.custom.MailSenderException;
import com.hong.hotdeal.exception.custom.SignupException;
import com.hong.hotdeal.util.AESUtil;
import com.hong.hotdeal.web.dto.request.signup.SignupRequestDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j(topic = "[SignupService]")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SignupService {

    private final AESUtil aesUtil;
    private final JavaMailSender javaMailSender;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    // 이메일 인증 요청 처리
    @Transactional
    @Async("emailExecutor")
    public void emailVerification(String email) {
        try {
            // 인증 코드 생성
            ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
            String code = generateCode();
            // email.code : code
            opsForValue.set(email + ":code", code, 5, TimeUnit.MINUTES);
            // email.status : false
            opsForValue.set(email + ":status", "false", 10, TimeUnit.MINUTES);

            sendEmailCode(email, code);
        } catch (Exception e) {
            log.error("이메일 인증 요청 오류 = {}", e.getMessage());
            throw new MailSenderException(ErrorCode.EMAIL_SENDER_FAIL);
        }
    }

    // 인증 코드 확인
    public String verifyCode(String email, String requestCode) {
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

        // 인증 코드 조회
        String code = redisTemplate.opsForValue().get(email + ":code");
        // 인증 상태 조회
        String status = redisTemplate.opsForValue().get(email + ":status");

        if (code == null) {
            log.error("이메일 인증 코드가 없음");
            throw new EmailVerificationException(ErrorCode.EMAIL_VERIFICATION_CODE_NOT_FOUND);
        }

        // 인증 상태가 null 이거나 true 이면
        if (status == null) {
            log.error("이메일 인증 상태가 없음");
            throw new EmailVerificationException(ErrorCode.EMAIL_VERIFICATION_STATUS_NOT_FOUND);
        }
        if (status.equals("true")) {
            log.error("이미 인증 됨");
            throw new EmailVerificationException(ErrorCode.EMAIL_VERIFICATION_STATUS_ALREADY_VERIFIED);
        }

        // 요청 코드랑 redis 코드랑 같은지 확인
        if (!requestCode.equals(code)) {
            log.error("인증 코드가 다름 : {}", code);
            throw new EmailVerificationException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }

        // 인증 상태 변경
        opsForValue.set(email + ":status", "true", 5, TimeUnit.MINUTES);
        return "이메일 인증 완료";
    }

    // 회원 가입 수행
    @Transactional
    public Long signup(SignupRequestDto requestDto) {
        String username = requestDto.getUsername();
        String email = requestDto.getEmail();
        String status = redisTemplate.opsForValue().get(email + ":status");

        // 이메일 인증을 받지 않은 상태
        if (status == null) {
            log.error("인증을 받지 않은 상태");
            throw new EmailVerificationException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 이메일 인증을 받지 않은 상태
        if (!status.equals("true")) {
            log.error("인증을 받지 않은 상태");
            throw new EmailVerificationException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // username 검사
        try {
            if(userRepository.existsByUsername(aesUtil.encrypt(username))){
                log.error("이미 존재하는 username = {}", username);
                throw new IllegalArgumentException("이미 존재 하는 username 입니다.");
            }
        }
        catch (Exception e){
            if(e instanceof IllegalArgumentException){
                log.error("username 중복 조회 오류 = {}", e.getMessage());
                throw new SignupException(ErrorCode.SIGNUP_EXISTS_USERNAME);
            }
            log.error("username 암호화 오류 = {}", e.getMessage());
            throw new SignupException(ErrorCode.CRYPTO_ENCRYPT_ERROR);
        }

        // email 검사
        try {
            if(userRepository.existsByEmail(aesUtil.encrypt(email))){
                log.error("이미 존재하는 email = {}", email);
                throw new IllegalArgumentException("이미 존재 하는 email 입니다.");
            }
        }
        catch (Exception e){
            if(e instanceof IllegalArgumentException){
                log.error("email 중복 조회 오류 = {}", e.getMessage());
                throw new SignupException(ErrorCode.SIGNUP_EXISTS_EMAIL);
            }
            log.error("email 암호화 오류 = {}", e.getMessage());
            throw new SignupException(ErrorCode.CRYPTO_ENCRYPT_ERROR);
        }

        // 회원가입할 때 기본적으로 ROLE -> USER로 설정
        try {
            // Address 생성
            Address address = Address.create(requestDto.getCity(), requestDto.getStreet(), requestDto.getZipCode());

            // User 생성
            User user = User.create(
                    aesUtil.encrypt(requestDto.getUsername()),
                    passwordEncoder.encode(requestDto.getPassword()),
                    aesUtil.encrypt(requestDto.getName()),
                    aesUtil.encrypt(requestDto.getPhoneNumber()),
                    address,
                    aesUtil.encrypt(requestDto.getEmail()),
                    Role.USER
            );
            // User 저장
            User savedUser = userRepository.save(user);
            return savedUser.getId();
        } catch (Exception e) {
            log.error("회원 가입 처리중 오류 : {}", e.getMessage());
            throw new SignupException(ErrorCode.CRYPTO_ENCRYPT_ERROR);
        }
    }


    private String generateCode() {
        return UUID.randomUUID().toString().substring(1, 7).toUpperCase();
    }

    private void sendEmailCode(String email, String code) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        mimeMessage.setFrom(email);
        mimeMessage.setRecipients(MimeMessage.RecipientType.TO, email);
        mimeMessage.setSubject("이메일 인증");
        String body = "";
        body += "<h3>요청하신 인증 번호입니다.</h3>";
        body += "<h1>" + code + "</h1>";
        body += "<h3>인증 확인란에 입력해주세요.</h3>";
        mimeMessage.setText(body, "UTF-8", "html");
        javaMailSender.send(mimeMessage);
    }
}
