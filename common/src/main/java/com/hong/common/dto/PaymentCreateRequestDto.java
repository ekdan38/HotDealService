package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequestDto {

    private String orderId;
    private Long userId;
    private BigDecimal amount;
    private LocalDateTime expireAt;
}
