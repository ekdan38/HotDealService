package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderFetchResponseDto {

    private boolean isFallback;
    private Long userId;
    private String orderId;
    private BigDecimal amount;
    private String orderStatus;

    public OrderFetchResponseDto(boolean isFallback) {
        this.isFallback = isFallback;
    }

    public OrderFetchResponseDto(Long userId, String orderId, BigDecimal amount, String orderStatus) {
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
        this.orderStatus = orderStatus;
    }
}
