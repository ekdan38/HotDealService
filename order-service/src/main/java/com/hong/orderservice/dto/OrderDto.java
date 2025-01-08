package com.hong.orderservice.dto;

import com.hong.orderservice.domain.Delivery;
import com.hong.orderservice.domain.OrderProduct;
import com.hong.orderservice.domain.status.OrderStatus;
import com.hong.orderservice.web.dto.OrderRequestDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class OrderDto {

    private Long id;
    private Long userId;
    private List<OrderProduct> orderProducts;
    private Delivery delivery;
    private OrderStatus orderStatus;

    private List<OrderRequestDto.OrderProductRequest> products;
    private String city;
    private String street;
    private String zipCode;
}
