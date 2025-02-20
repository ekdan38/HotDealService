package com.hong.productservice.web.controller;

import com.hong.common.dto.ResponseDto;
import com.hong.productservice.dto.wishlist.WishlistPagingResponseDto;
import com.hong.productservice.dto.wishlist.WishlistResponseDto;
import com.hong.productservice.service.wishlist.WishlistService;
import com.hong.productservice.web.dto.wishlist.WishlistRequestDto;
import com.hong.productservice.web.dto.wishlist.WishlistUpdateRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[WishlistController]")
@RequestMapping("/wishlists")
public class WishlistController {

    private final WishlistService wishlistService;

    // wishlist 에 product 등록
    @PostMapping
    public ResponseEntity<?> createWishlist(@RequestHeader("X-User-Id") Long userId,
                                            @RequestBody @Validated WishlistRequestDto requestDto,
                                            BindingResult bindingResult) {

        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("위시리스트 생성 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        WishlistResponseDto resultDto = wishlistService.createWishlist(userId, requestDto);

        // 응답 설정
        ResponseDto<WishlistResponseDto> responseDto = new ResponseDto<>("위시리스트 등록 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }


    // wishlist 커서 기반 페이징 조회
    @GetMapping
    public ResponseEntity<ResponseDto<WishlistPagingResponseDto>> getWishlists(@RequestHeader("X-User-Id") Long userId,
                                                                               @RequestParam(required = false) Long cursor,
                                                                               @RequestParam(required = false, defaultValue = "10") int size) {

        WishlistPagingResponseDto resultDto = wishlistService.getWishlists(userId, cursor, size);
        // 응답 설정
        ResponseDto<WishlistPagingResponseDto> responseDto = new ResponseDto<>("위시리스트 조회 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // wishlist 수정 (상품 수량 변경 포함)
    @PutMapping
    public ResponseEntity<?> updateWishlist(@RequestHeader("X-User-Id") Long userId,
                                            @RequestBody WishlistUpdateRequestDto requestDto){

        String result = wishlistService.updateWishlist(userId, requestDto.getUpdates());

        // 응답 설정
        ResponseDto<String> responseDto = new ResponseDto<>(result);
        return ResponseEntity.ok().body(responseDto);
    }
    // wishlist 삭제
    @DeleteMapping
    public ResponseEntity< ResponseDto<Long>> deleteWishlist(@RequestHeader("X-User-Id") Long userId) {

        Long wishlistId = wishlistService.deleteWishlist(userId);

        // 응답 설정
        ResponseDto<Long> responseDto = new ResponseDto<>("wishlist 삭제 성공", wishlistId);
        return ResponseEntity.ok().body(responseDto);
    }

}
