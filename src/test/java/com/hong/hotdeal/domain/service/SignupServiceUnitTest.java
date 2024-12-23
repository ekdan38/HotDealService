package com.hong.hotdeal.domain.service;

import com.hong.hotdeal.domain.Address;
import com.hong.hotdeal.service.SignupService;
import com.hong.hotdeal.util.AESUtil;
import com.hong.hotdeal.domain.User;
import com.hong.hotdeal.domain.status.Role;
import com.hong.hotdeal.repository.UserRepository;
import com.hong.hotdeal.exception.custom.EmailVerificationException;
import com.hong.hotdeal.exception.custom.MailSenderException;
import com.hong.hotdeal.exception.custom.SignupException;
import com.hong.hotdeal.web.dto.request.signup.SignupRequestDto;
import jakarta.mail.internet.MimeMessage;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignupServiceUnitTest {

    @Mock
    UserRepository userRepository;

    @Mock
    AESUtil aesUtil;

    @Mock
    JavaMailSender javaMailSender;

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    SignupService signupService;

    private SignupRequestDto requestDto;
    private String secretKey = "W/Tts3dODgVQ8d+SEd9e3a==";
    private final String emilCode = "A1B2C3";



    @BeforeEach
    void setUp() {
        requestDto = new SignupRequestDto
                ("testuser",
                        "testpassword",
                        "testname",
                        "123-1234-1234",
                        "City","동탄순환대로", "133333",
                        "test@test.com"
                );
//        signupService.setSecretKey(secretKey);
    }

    @Test
    @DisplayName("이메일 인증 요청 - 성공")
    public void emailVerification_Success() {
        //given
        givenForEmailVerification();
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        //when
        signupService.emailVerification(requestDto.getEmail());

        //then
        verify(redisTemplate, times(1)).opsForValue();
        verify(javaMailSender, times(1)).send((MimeMessage) any());
    }

    @Test
    @DisplayName("이메일 인증 요청 - 메일 발송 예외 발생시에 실패")
    public void emailVerification_Fail() {
        //given
        givenForEmailVerification();
        doThrow(new RuntimeException()).when(javaMailSender).send(any(MimeMessage.class));

        //when && then
        assertThatThrownBy(() -> signupService.emailVerification(requestDto.getEmail()))
                .isInstanceOf(MailSenderException.class);
    }

    @Test
    @DisplayName("이메일 인증 - 성공")
    public void verifyCode_Success(){
        //given
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(eq(requestDto.getEmail() + ":code"))).thenReturn(emilCode);
        when(valueOperations.get(eq(requestDto.getEmail() + ":status"))).thenReturn("false");
        doNothing().when(valueOperations).set(eq(requestDto.getEmail() + ":status"), anyString());
        //when
        String result = signupService.verifyCode(requestDto.getEmail(), emilCode);

        //then
        assertThat(result).isEqualTo("이메일 인증 완료");
    }

    @Test
    @DisplayName("이메일 인증 실패 - 인증 코드가 null")
    public void verifyCode_Fail_codeNull(){
        //given
        givenForVerifyCode(null, "false");

        //when && then
        assertThatThrownBy(() -> signupService.verifyCode(requestDto.getEmail(), emilCode))
                .isInstanceOf(EmailVerificationException.class);
    }

    @Test
    @DisplayName("이메일 인증 실패 - 인증 상태가 null")
    public void verifyCode_Fail_StatusNull(){
        //given
        givenForVerifyCode(emilCode, null);

        //when && then
        assertThatThrownBy(() -> signupService.verifyCode(requestDto.getEmail(), emilCode))
                .isInstanceOf(EmailVerificationException.class);
    }
    @Test

    @DisplayName("이메일 인증 실패 - 이미 인증된 상태")
    public void verifyCode_Fail_statusTrue(){
        //given
        givenForVerifyCode(emilCode, "true");

        //when && then
        assertThatThrownBy(() -> signupService.verifyCode(requestDto.getEmail(), emilCode))
                .isInstanceOf(EmailVerificationException.class);
    }

    @Test
    @DisplayName("이메일 인증 실패 - 인증 코드가 다를 때")
    public void verifyCode_Fail_mismatchCode(){
        //given
        String wrongEmailCode = "123456";
        givenForVerifyCode(wrongEmailCode, "false");

        //when && then
        assertThatThrownBy(() -> signupService.verifyCode(requestDto.getEmail(), emilCode))
                .isInstanceOf(EmailVerificationException.class);
    }

    @Test
    @DisplayName("회원 가입 - 성공")
    public void signup_Success() {
        //given
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(eq(requestDto.getEmail() + ":status"))).thenReturn("true");
        when(passwordEncoder.encode(anyString())).thenReturn("encodedValue");

        Address address = Address.create(requestDto.getCity(), requestDto.getStreet(), requestDto.getZipCode());
        User mockUser = User.create(requestDto.getUsername(),
                requestDto.getPassword(),
                requestDto.getName(),
                requestDto.getPhoneNumber(),
                address,
                requestDto.getEmail(),
                Role.USER);

        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        //when
        Long userId = signupService.signup(requestDto);

        //then
        Assertions.assertThat(userId).isEqualTo(null);
    }

    @Test
    @DisplayName("회원 가입 실패 - 이메일 인증을 받지 않은 상태")
    public void signup_Fail_NoVerifyCode() {
        //given
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(eq(requestDto.getEmail() + ":status"))).thenReturn("false");

        //when && then
        Assertions.assertThatThrownBy(() -> signupService.signup(requestDto)).isInstanceOf(EmailVerificationException.class);
    }

    @Test
    @DisplayName("회원 가입 실패 - 개인정보 암호화 처리 중 오류")
    public void signup_Fail_AesException() throws Exception {
        //given
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(eq(requestDto.getEmail() + ":status"))).thenReturn("true");

//        when(aesUtil.encrypt(anyString(), anyString())).thenThrow(new RuntimeException());
        when(aesUtil.encrypt(anyString())).thenThrow(new RuntimeException());

        //when && then
        Assertions.assertThatThrownBy(() -> signupService.signup(requestDto)).isInstanceOf(SignupException.class);
    }



    private void givenForEmailVerification(){
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        // redisTemplate.opsForValue() 호출하면 모킹된 객체 반환
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // redis 저장
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // 이메일 전송
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    private void givenForVerifyCode(String emailCode, String status){
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(eq(requestDto.getEmail() + ":code"))).thenReturn(emailCode);
        when(valueOperations.get(eq(requestDto.getEmail() + ":status"))).thenReturn(status);
    }
}