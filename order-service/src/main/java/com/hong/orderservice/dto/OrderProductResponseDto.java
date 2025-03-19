package com.hong.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hong.orderservice.domain.OrderProduct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductResponseDto {
    private Long productId;
    private Long hotDealProductId;
    private String productTitle;
    private Integer quantity;
    private Integer price;

    public OrderProductResponseDto(OrderProduct op) {
        this.productId = op.getProductId();
        this.hotDealProductId = op.getHotDealProductId();
        this.productTitle = op.getProductTitle();
        this.quantity = op.getQuantity();
        this.price = op.getPrice();
    }
}
