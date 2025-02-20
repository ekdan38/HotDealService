package com.hong.userservice.service;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.UserException;
import com.hong.userservice.AESUtil;
import com.hong.userservice.domain.Role;
import com.hong.userservice.domain.User;
import com.hong.userservice.domain.base.Address;
import com.hong.userservice.dto.SignupResponseDto;
import com.hong.userservice.jwt.JwtUtil;
import com.hong.userservice.repository.UserRepository;
import com.hong.userservice.web.dto.SignupRequestDto;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
@Slf4j(topic = "[UserServiceImpl]")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService{
    private final AESUtil aesUtil;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // 이메일 인증 코드 전송
    @Override
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
            log.debug("이메일 인증 코드 전송을 실패했습니다. errorMessage = {}", e.getMessage());
            throw new UserException(ErrorCode.EMAIL_SENDER_FAILED, e.getMessage());
        }
    }

    // 이메일 인증 코드 검증
    @Override
    public String verifyCode(String email, String requestCode) {
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

        // 인증 코드 조회
        String code = redisTemplate.opsForValue().get(email + ":code");
        // 인증 상태 조회
        String status = redisTemplate.opsForValue().get(email + ":status");

        // 이메일 인증 코드 검증
        validateEmailCode(email, requestCode, code, status);

        // 인증 상태 변경
        opsForValue.set(email + ":status", "true", 5, TimeUnit.MINUTES);
        return "이메일 인증 완료";
    }


    // 회원 가입
    @Override
    @Transactional
    public SignupResponseDto signup(SignupRequestDto requestDto) {
        String username = requestDto.getUsername();
        String email = requestDto.getEmail();

        // 이메일 인증 상태 검증
        validateEmailStatus(email);
        // username, email 중복 검증
        validateDuplicateUser(username, email);

        // user 생성, 저장
        User savedUser = createAndSaveUser(requestDto);

        return convertSignupResponseDto(savedUser);
    }

    // 토큰 재발급
    @Override
    public String reissueToken(HttpServletRequest request, HttpServletResponse response) {
        // Cookie 에서 refreshToken 추출
        String refresh = extractRefreshToken(request);

        // refreshToken 검증
        validateRefreshToken(refresh);

        Long userId = jwtUtil.getUserId(refresh);
        String username = jwtUtil.getUsername(refresh);
        String role = jwtUtil.getRole(refresh);

        // Access, Refresh Token 재발급 => TokenRotation
        String newAccess = jwtUtil.createJwt("access", userId, username, role, 600000L);
        String newRefresh = jwtUtil.createJwt("refresh", userId, username, role, 86400000L);

        // redis 에 refreshToken 저장
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        opsForValue.set(username + ":refresh", refresh, 24, TimeUnit.HOURS);

        // 응답 설정
        response.setHeader("Authorization", "Bearer " + newAccess);
        response.addCookie(createCookie("refresh", newRefresh));
        return "AccessToken, RefreshToken 재발급 완료.";
    }

    private void validateRefreshToken(String refresh) {
        // 만료 체크
        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {
            log.debug("만료된 RefreshToken 입니다. refreshToken = {}", refresh);
            throw new UserException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // 토큰이 refresh인지 확인 (발급시 페이로드에 명시)
        String category = jwtUtil.getCategory(refresh);

        if (!category.equals("refresh")) {
            log.debug("만료된 RefreshToken 입니다. = {}", refresh);
            throw new UserException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    private Cookie createCookie(String key, String value){
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60); // 24시간
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }

    // emailCode 생성
    private String generateCode() {
        return UUID.randomUUID().toString().substring(1, 7).toUpperCase();
    }

    // emailCode 메시지 작성
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

    // 이메일 인증 코드 검증
    private void validateEmailCode(String email, String requestCode, String code, String status) {
        if (code == null) {
            log.debug("해당 이메일로 생성 된 인증 코드가 없습니다. email = {}", email);
            throw new UserException(ErrorCode.EMAIL_EMPTY_CODE, email);
        }
        // 인증 상태가 null 이거나 true 이면
        if (status == null) {
            log.debug("이메일 인증 상태가 존재하지 않습니다. email = {}", email);
            throw new UserException(ErrorCode.EMAIL_VERIFICATION_STATUS_NOT_FOUND, email);
        }
        if (status.equals("true")) {
            log.debug("이미 이메일 인증을 완료 했습니다. email = {}", email);
            throw new UserException(ErrorCode.EMAIL_VERIFICATION_STATUS_ALREADY_VERIFIED, email);
        }

        // 요청 코드랑 redis 코드랑 같은지 확인
        if (!requestCode.equals(code)) {
            log.debug("이메일 인증 코드가 다릅니다. email = {}", email);
            throw new UserException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH, email);
        }
    }

    // email 인증 상태 검증
    private void validateEmailStatus(String email) {
        // 이메일 인증 상태 검증
        String status = redisTemplate.opsForValue().get(email + ":status");
        if (status == null || !status.equals("true")) {
            log.debug("이메일 인증을 받지 않았습니다. email = {}", email);
            throw new UserException(ErrorCode.EMAIL_VERIFICATION_NOT_VERIFIED, email);
        }
    }

    // username, email 중복 검증
    private void validateDuplicateUser(String username, String email) {
        // username 검사
        try {
            if(userRepository.existsByUsername(aesUtil.encrypt(username))){
                log.debug("이미 존재하는 username 입니다. username = {}", username);
                throw new UserException(ErrorCode.USER_USERNAME_ALREADY_EXISTS, username);
            }
        }
        catch (Exception e){
            if(e instanceof UserException) throw (UserException) e;
            log.debug("암호화 처리중 오류가 발생했습니다. 대상 = {}, 암호화 오류 = {}",username, e.getMessage());
            throw new UserException(ErrorCode.CRYPTO_ENCRYPT_ERROR);
        }

        // email 검사
        try {
            if(userRepository.existsByEmail(aesUtil.encrypt(email))){
                log.debug("이미 존재하는 email 입니다. email = {}", email);
                throw new UserException(ErrorCode.USER_EMAIL_ALREADY_EXISTS, email);
            }
        }
        catch (Exception e){
            if(e instanceof UserException) throw (UserException) e;
            log.debug("암호화 처리중 오류가 발생했습니다. 대상 = {}, 암호화 오류 = {}",email, e.getMessage());
            throw new UserException(ErrorCode.CRYPTO_ENCRYPT_ERROR);
        }
    }

    // user 생성, 저장
    private User createAndSaveUser(SignupRequestDto requestDto) {
        // 회원가입할 때 기본적으로 ROLE -> USER로 설정
        try {
            User user = User.create(
                    aesUtil.encrypt(requestDto.getUsername()),
                    passwordEncoder.encode(requestDto.getPassword()),
                    aesUtil.encrypt(requestDto.getName()),
                    aesUtil.encrypt(requestDto.getPhoneNumber()),
                    Address.create(
                            aesUtil.encrypt(requestDto.getCity()),
                            aesUtil.encrypt(requestDto.getStreet()),
                            aesUtil.encrypt(requestDto.getZipCode())
                    ),
                    aesUtil.encrypt(requestDto.getEmail()),
                    Role.USER
            );
            return userRepository.save(user);
        }
        catch (Exception e){
            log.debug("암호화 처리중 오류가 발생했습니다. 대상 = {}, 암호화 오류 = {}",requestDto, e.getMessage());
            throw new UserException(ErrorCode.CRYPTO_ENCRYPT_ERROR);
        }
    }

    // 회원 가입 응답 Dto 변환
    private SignupResponseDto convertSignupResponseDto(User savedUser) {
        try {
            Address address = savedUser.getAddress();
            return new SignupResponseDto(
                    savedUser.getId(),
                    aesUtil.decrypt(savedUser.getUsername()),
                    aesUtil.decrypt(savedUser.getName()),
                    aesUtil.decrypt(savedUser.getPhoneNumber()),
                    aesUtil.decrypt(savedUser.getEmail()),
                    Address.create(
                            aesUtil.decrypt(address.getCity()),
                            aesUtil.decrypt(address.getStreet()),
                            aesUtil.decrypt(address.getZipCode())
                    ));
        }
        catch (Exception e){
            log.debug("복호화 처리중 오류가 발생했습니다. 대상 = {}, 암호화 오류 = {}", savedUser, e.getMessage());
            throw new UserException(ErrorCode.CRYPTO_DECRYPT_ERROR);
        }
    }

    // Cookie 에서 refreshToken 추출
    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.debug("RefreshToken 이 존재 하지 않습니다.");
            throw new UserException(ErrorCode.REFRESH_TOKEN_NULL);
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refresh")) {
                return cookie.getValue();
            }
        }
        log.debug("RefreshToken 이 존재 하지 않습니다.");
        throw new UserException(ErrorCode.REFRESH_TOKEN_NULL);
    }
}
