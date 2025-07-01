package com.hong.userservice.web;

import com.hong.common.dto.UserCartDeleteRequestDto;
import com.hong.common.dto.UserCartDeleteResponseDto;
import com.hong.userservice.service.CartItemApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[CartApiController]")
@RequestMapping("user-service/protect/users/carts")
public class CartApiController {

    private final CartItemApiService cartItemApiService;

    // 특정 상품 cart 에서 삭제
    @PostMapping
    public ResponseEntity<UserCartDeleteResponseDto> deleteCartItems(@RequestBody UserCartDeleteRequestDto requestDto){

        UserCartDeleteResponseDto resultDto = cartItemApiService.deleteCartItems(requestDto);
        return ResponseEntity.ok().body(resultDto);
    }
}
