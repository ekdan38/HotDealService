package com.hong.paymentservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPrepareRequestDto {

    @NotBlank(message = "orderId 는 필수입니다.")
    private String orderId;

    @NotNull(message = "amount 는 필수입니다.")
    private BigDecimal amount;
}
