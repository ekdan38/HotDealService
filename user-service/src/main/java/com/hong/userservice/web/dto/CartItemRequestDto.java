package com.hong.userservice.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CartItemRequestDto {

    @NotNull(message = "productId 는 필수입니다.")
    private Long productId;

    @NotNull(message = "quantity 는 필수입니다.")
    @Min(value = 1, message = "quantity 는 최소 1개 이상이어야 합니다.")
    @Max(value = 99, message = "quantity 는 최대 99개까지 담을 수 있습니다.")
    private Integer quantity;
}
