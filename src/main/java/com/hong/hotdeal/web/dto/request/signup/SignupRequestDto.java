package com.hong.hotdeal.web.dto.request.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignupRequestDto {

    @NotBlank(message = "username은 필수입니다.")
    @Size(min = 6, max = 12, message = "username은 6글자 이상 12글자 이하입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "username은 영어 대소문자와 숫자만 허용됩니다.")
    private String username;

    @NotBlank(message = "password는 필수입니다.")
    @Size(min = 6, max = 12, message = "password는 6글자 이상 12글자 이하입니다.")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "password는 영어 대소문자만 허용됩니다.")
    private String password;

    @NotBlank(message = "name은 필수입니다.")
    @Size(min = 2, max = 10, message = "name은 2글자 이상 10글자 이하입니다.")
    @Pattern(regexp = "^[가-힣]+$", message = "이름은 한글만 가능합니다.")
    private String name;

    @NotBlank(message = "phoneNumber는 필수입니다.")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "phoneNumber은 전화번호 형식을 따라야합니다.")
    private String phoneNumber;

    @NotBlank(message = "address는 필수입니다.")
    private String address;

    @NotBlank(message = "email은 필수입니다.")
    @Email(message = "email은 이메일 형식을 따라야합니다.")
    private String email;
}
