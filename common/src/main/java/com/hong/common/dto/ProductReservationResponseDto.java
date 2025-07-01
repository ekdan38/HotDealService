package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReservationResponseDto {
    private boolean fallback;
    private String reservationToken;
    private List<ReservedProductDto> products;

    public ProductReservationResponseDto(String reservationToken, List<ReservedProductDto> products) {
        this.fallback = false;
        this.reservationToken = reservationToken;
        this.products = products;
    }

    public ProductReservationResponseDto(boolean fallback) {
        this.fallback = fallback;
        this.reservationToken = null;
        this.products = Collections.emptyList();
    }
}
