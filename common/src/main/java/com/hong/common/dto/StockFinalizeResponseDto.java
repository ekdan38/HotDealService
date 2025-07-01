package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockFinalizeResponseDto {
    private Boolean success;
    private Boolean retriable;

    public Boolean isSuccess() {
        return success;
    }

    public Boolean isRetriable() {
        return retriable;
    }
}
