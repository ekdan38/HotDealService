package com.hong.paymentservice.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPerformRequestDto {

    private String sessionId;
    private BigDecimal amount;
}
