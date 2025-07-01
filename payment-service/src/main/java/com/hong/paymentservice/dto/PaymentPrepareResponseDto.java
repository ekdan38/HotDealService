package com.hong.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPrepareResponseDto {

    private String sessionId;
    private String redirectUrl;
}
