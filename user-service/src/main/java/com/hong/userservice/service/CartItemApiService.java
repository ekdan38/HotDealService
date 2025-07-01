package com.hong.userservice.service;

import com.hong.common.dto.UserCartDeleteRequestDto;
import com.hong.common.dto.UserCartDeleteResponseDto;
import com.hong.userservice.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[CartItemApiService]")
@Transactional
public class CartItemApiService {
    private final CartItemRepository cartItemRepository;

    public UserCartDeleteResponseDto deleteCartItems(UserCartDeleteRequestDto requestDto){
        Long userId = requestDto.getUserId();
        List<Long> productIds = requestDto.getProductIds();

        cartItemRepository.deleteByUserIdAndProductIdIn(userId, productIds);
        return new UserCartDeleteResponseDto(true, false);
    }
}
