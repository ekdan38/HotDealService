package com.hong.hotdeal.web.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hong.hotdeal.domain.Product;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // Null 값을 가진 필드는 직렬화에서 제외
public class ProductResponseDto {
    private Long id;
    private String title;
    private Integer price;
    private Integer stock;
    private String category;

    public ProductResponseDto(Product product) {
        this.id = product.getId();
        this.title = product.getTitle();
        this.price = product.getPrice();
        this.stock = product.getStock();
    }

    public ProductResponseDto(Product product, String category) {
        this.id = product.getId();
        this.title = product.getTitle();
        this.price = product.getPrice();
        this.stock = product.getStock();
        this.category = category;
    }
}
