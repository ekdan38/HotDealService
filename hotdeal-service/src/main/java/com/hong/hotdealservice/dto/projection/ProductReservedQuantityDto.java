package com.hong.hotdealservice.dto.projection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReservedQuantityDto {
    private Long productId;
    private Long reservedQuantity;
}
