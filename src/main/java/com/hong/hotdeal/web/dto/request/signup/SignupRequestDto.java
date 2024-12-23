package com.hong.hotdeal.web.dto.request.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDto {

    @NotBlank(message = "username 은 필수입니다.")
    @Size(min = 6, max = 12, message = "username 은 6글자 이상 12글자 이하입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "username 은 영어 대소문자와 숫자만 허용됩니다.")
    private String username;

    @NotBlank(message = "password 는 필수입니다.")
    @Size(min = 6, max = 12, message = "password 는 6글자 이상 12글자 이하입니다.")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "password 는 영어 대소문자만 허용됩니다.")
    private String password;

    @NotBlank(message = "nam e은 필수입니다.")
    @Size(min = 2, max = 10, message = "name 은 2글자 이상 10글자 이하입니다.")
    @Pattern(regexp = "^[가-힣]+$", message = "이름은 한글만 가능합니다.")
    private String name;

    @NotBlank(message = "phoneNumber 는 필수입니다.")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "phoneNumber 은 전화번호 형식을 따라야합니다.")
    private String phoneNumber;

    @NotBlank(message = "city 는 필수입니다.")
    private String city;

    @NotBlank(message = "street 는 필수입니다.")
    private String street;

    @NotBlank(message = "zipCode 는 필수입니다.")
    private String zipCode;

    @NotBlank(message = "email 은 필수입니다.")
    @Email(message = "email 은 이메일 형식을 따라야합니다.")
    private String email;
}
