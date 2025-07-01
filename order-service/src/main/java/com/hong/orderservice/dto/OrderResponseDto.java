package com.hong.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.status.DeliveryStatus;
import com.hong.orderservice.domain.status.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private String orderId;
    private Long userId;
    private BigDecimal totalPrice;
    private OrderStatus orderStatus;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime orderDate;
    private List<OrderProductResponseDto> orderProducts;

    public OrderResponseDto(String orderId, Long userId, BigDecimal amount) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalPrice = amount;
    }

    public OrderResponseDto(Order order) {
        this.orderId = order.getId();
        this.userId = order.getUserId();
        this.totalPrice = order.getAmount();
        this.orderStatus = order.getStatus();
        this.deliveryStatus = order.getDelivery().getStatus();
        this.orderDate = order.getCreatedAt();
    }
    public OrderResponseDto(Order order, List<OrderProductResponseDto> orderProducts) {
        this.orderId = order.getId();
        this.userId = order.getUserId();
        this.totalPrice = order.getAmount();
        this.orderStatus = order.getStatus();
        this.deliveryStatus = order.getDelivery().getStatus();
        this.orderDate = order.getCreatedAt();
        this.orderProducts = orderProducts;
    }
}