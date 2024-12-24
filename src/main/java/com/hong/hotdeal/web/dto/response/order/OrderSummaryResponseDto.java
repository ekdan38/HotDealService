package com.hong.hotdeal.web.dto.response.order;

import com.hong.hotdeal.domain.status.DeliveryStatus;
import com.hong.hotdeal.domain.status.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderSummaryResponseDto {
    private Long orderId;
    private Integer totalPrice;
    private OrderStatus orderStatus;
    private DeliveryStatus deliveryStatus;
}
