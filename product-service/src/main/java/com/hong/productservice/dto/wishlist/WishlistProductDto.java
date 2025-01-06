package com.hong.productservice.dto.wishlist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistProductDto {

    private Long id;
    private String title;
    private Integer price;
    private Integer quantity;

}
