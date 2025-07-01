package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateRequestDto {

    private Long userId;
    private String orderId;
    private Boolean success;

    public Boolean isSuccess() {
        return success;
    }
}
