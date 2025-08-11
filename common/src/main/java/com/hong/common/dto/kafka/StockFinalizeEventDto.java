package com.hong.common.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class StockFinalizeEventDto {
    private String orderId;
    private Long userId;
    private boolean isPaySuccess;
}
