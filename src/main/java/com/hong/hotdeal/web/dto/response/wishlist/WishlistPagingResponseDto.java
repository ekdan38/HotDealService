package com.hong.hotdeal.web.dto.response.wishlist;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WishlistPagingResponseDto {

    private Long wishlistId;

    private Long cursor;

    private List<WishlistResponseDto> products;
}
