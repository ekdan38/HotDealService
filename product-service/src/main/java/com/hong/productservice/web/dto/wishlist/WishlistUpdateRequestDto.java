package com.hong.productservice.web.dto.wishlist;

import lombok.Data;

import java.util.List;

@Data
public class WishlistUpdateRequestDto {

    private List<WishlistProductUpdate> updates;

    @Data
    public static class WishlistProductUpdate{
        private Long productId;
        // update or delete
        private String method;
        private Integer quantity;
    }
}
