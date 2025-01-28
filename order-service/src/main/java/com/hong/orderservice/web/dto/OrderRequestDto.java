package com.hong.orderservice.web.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderRequestDto {

    @NotEmpty(message = "products 는 최소 1개 이상 이어야 합니다.")
    private List<OrderProductRequest> products;

    @NotBlank(message = "city 는 필수 입니다.")
    @Size(min = 2, max = 20, message = "city 는 2 글자에서 20 글자입니다.")
    private String city;

    @NotBlank(message = "street 는 필수 입니다.")
    @Size(min = 2, max = 30, message = "street 는 2 글자에서 30 글자입니다.")
    private String street;

    @NotBlank(message = "zipCode 는 필수 입니다.")
    @Size(min = 2, max = 10, message = "zipCode 는 2 글자에서 10 글자입니다.")
    private String zipCode;

    @Data
    @AllArgsConstructor
    public static class OrderProductRequest{
        // hotDeal 상품이면 hotDealId, productId 는 hotDealProductId
        // 일반 상품이면 productId 는 productId
        @NotNull(message = "productId 는 필수입니다.")
        private Long productId;
        private Long hotDealId;
        private Long hotDealProductId;
        @NotNull(message = "quantity 는 필수입니다.")
        @Positive(message = "quantity 는 양수여야 합니다.")
        private Integer quantity;
    }
}
