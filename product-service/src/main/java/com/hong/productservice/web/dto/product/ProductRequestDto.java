package com.hong.productservice.web.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDto {

    @NotBlank(message = "title은 필수입니다.")
    @Size(min = 2, max = 20, message = "title은 2 글자에서 20 글자입니다.")
    private String title;

    @NotNull(message = "price는 필수입니다.")
    @Min(value = 0, message = "price는 0 이상이어야 합니다.")
    private Integer price;

    @NotNull(message = "stock은 필수입니다.")
    @Min(value = 0, message = "stock은 0 이상이어야 합니다.")
    private Integer stock;

    @NotEmpty(message = "categoryIds는 최소 1개 이상이어야 합니다.")
    private List<Long> categoryIds;
}
