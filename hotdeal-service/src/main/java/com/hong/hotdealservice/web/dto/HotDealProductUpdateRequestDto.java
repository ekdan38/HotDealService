package com.hong.hotdealservice.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotDealProductUpdateRequestDto {

    @Positive(message = "hotDealProductId 는 양수여야 합니다.")
    private Long hotDealProductId;

    @NotNull(message = "productId 는 필수입니다.")
    @Positive(message = "productId 는 양수여야 합니다.")
    private Long productId;

    @NotNull(message = "quantity 는 필수입니다.")
    @Positive(message = "quantity 는 양수여야 합니다.")
    private Integer quantity;

    @NotNull(message = "discountRate 는 필수입니다.")
    @DecimalMin(value = "0.0", message = "discountRate 는 0.0 이상이어야 합니다.")
    @DecimalMax(value = "1.0", message = "discountRate 는 1.0 이하이어야 합니다.")
    private Double discountRate;
}
