package com.hong.productservice.web.dto.cateogry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryRequestDto {

    @NotBlank(message = "title은 필수입니다.")
    @Size(min = 2, max = 10, message = "title은 2 글자에서 10 글자입니다.")
    private String title;

}
