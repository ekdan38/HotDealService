package com.hong.hotdeal.web.dto.request.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EmailVerificationRequestDto {

    @NotBlank(message = "email은 필수입니다.")
    @Email(message = "email은 이메일 형식을 따라야합니다.")
    private String email;
}
