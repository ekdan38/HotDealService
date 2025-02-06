package com.hong.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hong.orderservice.domain.status.DeliveryStatus;
import com.hong.orderservice.domain.status.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
@AllArgsConstructor
@JsonPropertyOrder({"orderId", "userId", "totalPrice", "orderStatus", "deliveryStatus", "orderDate", "products"})
public class OrderResponseDto {
    private Long orderId;
    private Long userId;
    private Integer totalPrice;
    private OrderStatus orderStatus;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime orderDate;
    private LocalDateTime paidAt;
    private List<OrderProductDto> products;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Data
    @AllArgsConstructor
    @JsonPropertyOrder({"productId", "productTitle", "quantity", "price"})
    public static class OrderProductDto {
        private Long productId;
        private Long hotDealId;
        private Long hotDealProductId;
        private String productTitle;
        private Integer quantity;
        private Integer price;

        public OrderProductDto(Long productId, Long hotDealId, String productTitle, Integer quantity, Integer price) {
            this.productId = productId;
            this.hotDealId = hotDealId;
            this.productTitle = productTitle;
            this.quantity = quantity;
            this.price = price;
        }
    }
}