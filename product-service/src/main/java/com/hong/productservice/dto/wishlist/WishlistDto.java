package com.hong.productservice.dto.wishlist;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WishlistDto {

    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
}
