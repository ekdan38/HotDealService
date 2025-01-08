package com.hong.productservice.service.wishlist;

import com.hong.productservice.dto.wishlist.WishlistResponseDto;
import com.hong.productservice.dto.wishlist.WishlistDto;
import com.hong.productservice.dto.wishlist.WishlistPagingResponseDto;
import com.hong.productservice.web.dto.wishlist.WishlistRequestDto;
import com.hong.productservice.web.dto.wishlist.WishlistUpdateRequestDto;

import java.util.List;

public interface WishlistService {

    // wishlist 에 product 등록
    WishlistResponseDto createWishlist(Long userId, WishlistRequestDto requestDto);

    // wishlist 커서 기반 페이징 조회
    WishlistPagingResponseDto getWishlists(Long userId, Long cursor, int size);

    // wishlist 수정 (상품 수량 변경 포함)
    String updateWishlist(Long userId, List<WishlistUpdateRequestDto.WishlistProductUpdate> updates);

    // wishlist 삭제
    Long deleteWishlist(Long userId);
}
