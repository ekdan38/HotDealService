package com.hong.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntryResponseDto {

    private Long paymentId;
    private Long orderId;
    private Integer amount;
    private LocalDateTime expiresAt;
}
