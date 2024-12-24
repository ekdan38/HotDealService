package com.hong.hotdeal.web.controller;

import com.hong.hotdeal.domain.User;
import com.hong.hotdeal.jwt.CustomUserDetails;
import com.hong.hotdeal.service.WishlistService;
import com.hong.hotdeal.web.dto.request.wishlist.WishlistUpdateRequestDto;
import com.hong.hotdeal.web.dto.response.ResponseDto;
import com.hong.hotdeal.web.dto.response.wishlist.WishlistPagingResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wishlists")
public class WishlistController {

    private final WishlistService wishlistService;

    // wishlist 에 상품 추가
    @PostMapping
    public ResponseEntity<ResponseDto<HashMap<String, Long>>> addWishlist(@RequestParam("productId") Long productId,
                                                                          @RequestParam("quantity") int quantity,
                                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long result = wishlistService.saveProductToWishlist(userDetails.getUser().getId(), productId, quantity);

        // 응답 설정
        HashMap<String, Long> data = new HashMap<>();
        data.put("productId", result);
        ResponseDto<HashMap<String, Long>> responseDto = new ResponseDto<>("wishlist 에 등록 성공.", data);
        return ResponseEntity.ok().body(responseDto);
    }

    // wishlist 페이징
    @GetMapping
    public ResponseEntity<ResponseDto<WishlistPagingResponseDto>> getWishlists(@RequestParam(value = "cursor", required = false) Long cursor,
                                                                               @RequestParam(value = "size", defaultValue = "10", required = false) int size,
                                                                               @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (cursor == null) cursor = Long.MAX_VALUE;
        WishlistPagingResponseDto wishlists = wishlistService.getWishlists(userDetails.getUser().getId(), cursor, size);

        // 응답 설정
        ResponseDto<WishlistPagingResponseDto> responseDto = new ResponseDto<>("wishlist 조회 성공.", wishlists);
        return ResponseEntity.ok().body(responseDto);
    }

    // wishlist 항목 수정 (수량 포함)
    @PutMapping
    public ResponseEntity<ResponseDto<String>> updateWishlist(@RequestBody WishlistUpdateRequestDto requestDto,
                                            @AuthenticationPrincipal CustomUserDetails userDetails){

        String result = wishlistService.updateWishlist(userDetails.getUser().getId(), requestDto.getUpdates());

        // 응답 설정
        ResponseDto<String> responseDto = new ResponseDto<>(result);
        return ResponseEntity.ok().body(responseDto);
    }

    // wishlist 삭제
    @DeleteMapping
    public ResponseEntity<ResponseDto<HashMap<String, Long>>> deleteWishlist(@AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        Long wishlistId = wishlistService.deleteWishlist(userId);

        // 응답 설정
        HashMap<String, Long> data = new HashMap<>();
        data.put("wishlistId", wishlistId);
        ResponseDto<HashMap<String, Long>> responseDto = new ResponseDto<>("wishlist 삭제 성공", data);
        return ResponseEntity.ok().body(responseDto);
    }
}
