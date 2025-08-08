package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateResponseDto {
    private Long paymentId;
    private boolean success = false;
    private boolean retriable = false;

    public PaymentCreateResponseDto(boolean retriable) {
        this.success = false;
        this.retriable = retriable;
    }

    public PaymentCreateResponseDto(Long paymentId, boolean success) {
        this.paymentId = paymentId;
        this.success = success;
        this.retriable = false;
    }
}
