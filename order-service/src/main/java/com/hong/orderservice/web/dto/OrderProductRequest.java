package com.hong.orderservice.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductRequest {
    @NotNull(message = "productId 는 필수입니다.")
    private Long productId;

    @NotNull(message = "quantity 는 필수입니다.")
    @Positive(message = "quantity 는 양수여야 합니다.")
    private Integer quantity;
}



