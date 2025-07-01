package com.hong.userservice.service;

import com.hong.userservice.dto.CartItemResponseDto;
import com.hong.userservice.web.dto.CartItemRequestDto;
import com.hong.userservice.web.dto.CartItemUpdateRequestDto;

public interface CartItemService {

    // 상품 추가
    CartItemResponseDto createCartItem(Long userId, CartItemRequestDto requestDto);

    // cart 조회
    CartItemResponseDto getCartItems(Long userId);

    // cart 수정
    CartItemResponseDto updateCartItems(Long userId, CartItemUpdateRequestDto requestDto);
}
