package com.hong.productservice.web.dto.wishlist;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WishlistRequestDto {

    @NotNull(message = "productId 는 필수입니다.")
    private Long productId;

    @NotNull(message = "quantity 는 필수입니다.")
    private Integer quantity;
}
