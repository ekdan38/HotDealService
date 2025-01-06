package com.hong.productservice.dto.wishlist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponseDto {

    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
}
