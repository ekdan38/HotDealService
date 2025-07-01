package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservedProductDto {
    private Long productId;
    private String productTitle;
    private Integer reservedQuantity;
    private BigDecimal price;
}
