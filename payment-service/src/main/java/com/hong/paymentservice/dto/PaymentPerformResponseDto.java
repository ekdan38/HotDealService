package com.hong.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPerformResponseDto {
    // SUCCESS, FAIL
    private String result;
    private String transactionId;
}
