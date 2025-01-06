package com.hong.productservice.dto.wishlist;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hong.productservice.dto.product.ProductResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"wishlistId", "nextCursor", "products"})
public class WishlistPagingResponseDto {

    private Long wishlistId;
    private Long nextCursor;
    private List<WishlistProductDto> products;

    public WishlistPagingResponseDto(Long wishlistId, Long nextCursor) {
        this.wishlistId = wishlistId;
        this.nextCursor = nextCursor;
    }
}
