package com.hong.productservice.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"nextCursor", "products"})
public class ProductPagingResponseDto {

    private Long nextCursor;
    @JsonProperty("products")
    private List<ProductResponseDto> productResponseDtos;
}
