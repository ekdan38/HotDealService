package com.hong.productservice.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@JsonPropertyOrder({"nextCursor", "products"})
public class ProductPagingResponseDto {

    private Long nextCursor;
    @JsonProperty("products")
    private List<ProductResponseDto> productResponseDtos;
}
