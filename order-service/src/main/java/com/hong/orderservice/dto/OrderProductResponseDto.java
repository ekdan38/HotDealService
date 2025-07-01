package com.hong.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hong.orderservice.domain.OrderProduct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductResponseDto {
    private Long productId;
    private String productTitle;
    private Integer quantity;
    private BigDecimal price;

    public OrderProductResponseDto(OrderProduct op) {
        this.productId = op.getProductId();
        this.productTitle = op.getTitle();
        this.quantity = op.getQuantity();
        this.price = op.getPrice();
    }
}
