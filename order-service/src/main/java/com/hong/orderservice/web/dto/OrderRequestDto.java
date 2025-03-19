package com.hong.orderservice.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDto {

    @NotBlank(message = "city 는 필수 입니다.")
    @Size(min = 2, max = 20, message = "city 는 2 글자에서 20 글자입니다.")
    private String city;

    @NotBlank(message = "street 는 필수 입니다.")
    @Size(min = 2, max = 30, message = "street 는 2 글자에서 30 글자입니다.")
    private String street;

    @NotBlank(message = "zipCode 는 필수 입니다.")
    @Size(min = 2, max = 10, message = "zipCode 는 2 글자에서 10 글자입니다.")
    private String zipCode;

    @NotEmpty(message = "products 는 최소 1개 이상 이어야 합니다.")
    private List<@Valid OrderProductRequest> products;
}
