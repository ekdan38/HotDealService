package com.hong.productservice.dto.wishlist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistUpdateResponseDto {

    private Long id;
    private Long userId;
    private List<WishlistProductDto> updates;
}
