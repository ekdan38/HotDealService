package com.hong.hotdeal.web.dto.response.wishlist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WishlistResponseDto {

    @JsonIgnore
    private Long wishlistId;

    @JsonIgnore
    private Long wishlistProductId;

    private Long productId;

    private String title;

    private Integer quantity;

    private String category;
}
