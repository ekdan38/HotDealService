package com.hong.hotdeal.web.dto.response.order;

import com.hong.hotdeal.domain.OrderProduct;
import com.hong.hotdeal.domain.status.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderResponseDto {
    private Long orderId;
    private List<OrderProductDto> products;
    private int totalPrice;
    private DeliveryStatus deliveryStatus;

    @Data
    public static class OrderProductDto {
        private Long productId;
        private String productTitle;
        private Integer quantity;
        private Integer price;

        public OrderProductDto(OrderProduct orderProduct) {
            this.productId = orderProduct.getProduct().getId();
            this.productTitle = orderProduct.getProduct().getTitle();
            this.quantity = orderProduct.getQuantity();
            this.price = orderProduct.getPrice();
        }
    }
}