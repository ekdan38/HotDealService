package com.hong.common.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentResultEventDto {

    private String orderId;
    private Long userId;
    private boolean success;
    @Nullable
    private LocalDateTime paidAt;
}