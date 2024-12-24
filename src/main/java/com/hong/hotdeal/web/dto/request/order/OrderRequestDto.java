package com.hong.hotdeal.web.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequestDto {

    private List<OrderProductRequest> products;
    private String city;
    private String street;
    private String zipCode;

    @Data
    public static class OrderProductRequest{
        private Long productId;
        private Integer quantity;
    }
}
