package com.hong.hotdeal.web.dto.request.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
public class EmailVerifyRequestDto {

    @NotBlank(message = "email은 필수입니다.")
    @Email(message = "email은 이메일 형식을 따라야합니다.")
    private String email;

    @NotNull(message = "code는 필수입니다.")
    @Length(min = 6, max = 6, message = "코드는 6자리입니다.")
    private String code;
}
