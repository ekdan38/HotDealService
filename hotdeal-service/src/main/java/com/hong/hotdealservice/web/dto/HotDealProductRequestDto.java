package com.hong.hotdealservice.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotDealProductRequestDto {

    @NotNull(message = "title 은 필수입니다.")
    private String title;

    @NotNull(message = "price 는 필수입니다.")
    @Positive(message = "price 는 양수만 허용됩니다.")
    private BigDecimal price;

    @NotNull(message = "stock 은 필수입니다.")
    @Positive(message = "stock 은 양수만 허용됩니다.")
    private Integer stock;
}
