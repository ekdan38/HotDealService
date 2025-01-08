package com.hong.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hong.orderservice.domain.status.DeliveryStatus;
import com.hong.orderservice.domain.status.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@JsonPropertyOrder({"orderId", "userId", "totalPrice", "orderStatus", "deliveryStatus", "orderDate", "products"})
public class OrderResponseDto {
    private Long orderId;
    private Long userId;
    private int totalPrice;
    private OrderStatus orderStatus;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime orderDate;
    private List<OrderProductDto> products;

    @Data
    @AllArgsConstructor
    @JsonPropertyOrder({"productId", "productTitle", "quantity", "price"})
    public static class OrderProductDto {
        private Long productId;
        private String productTitle;
        private Integer quantity;
        private Integer price;
    }
}