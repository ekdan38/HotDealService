package com.hong.userservice.web;

import com.hong.common.dto.ResponseDto;
import com.hong.userservice.dto.CartItemResponseDto;
import com.hong.userservice.service.CartItemService;
import com.hong.userservice.web.dto.CartItemRequestDto;
import com.hong.userservice.web.dto.CartItemUpdateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[CartController]")
@RequestMapping("protect/users/carts")
public class CartController {

    private final CartItemService cartItemService;

    // cart 에 상품 추가
    @PostMapping
    public ResponseEntity<?> createCartItem(@RequestHeader("X-User-Id") Long userId,
                                            @RequestBody @Validated CartItemRequestDto requestDto,
                                            BindingResult bindingResult) {
        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("장바구니 상품 추가 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        CartItemResponseDto resultDto = cartItemService.createCartItem(userId, requestDto);
        // 응답 설정
        ResponseDto<CartItemResponseDto> responseDto = new ResponseDto<>("장바구니 상품 추가 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // cart 조회
    @GetMapping
    public ResponseEntity<ResponseDto<CartItemResponseDto>> getCarts(@RequestHeader("X-User-Id") Long userId) {

        CartItemResponseDto resultDto = cartItemService.getCartItems(userId);
        // 응답 설정
        ResponseDto<CartItemResponseDto> responseDto = new ResponseDto<>("장바구니 조회 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // cart 수정
    @PutMapping
    public ResponseEntity<?> updateCart(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody @Valid CartItemUpdateRequestDto requestDto,
                                          BindingResult bindingResult) {
        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("장바구니 수정 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        CartItemResponseDto resultDto = cartItemService.updateCartItems(userId, requestDto);
        // 응답 설정
        ResponseDto<CartItemResponseDto> responseDto = new ResponseDto<>("장바구니 수정 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }
}
