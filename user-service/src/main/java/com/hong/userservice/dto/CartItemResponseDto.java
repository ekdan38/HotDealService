package com.hong.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDto {

    private Long userId;

    private List<CartItemDto> cartItems;
}
