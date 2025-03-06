package com.hong.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderFetchResponseDto {

    private Long userId;
    private Long orderId;
    private Integer amount;
    private String status;
    private List<orderHotDealProductDto> hotDealProducts;
    private List<OrderProductDto> products;

    @JsonIgnore
    public boolean isEmpty(){
        return (userId == null && orderId == null && amount == null
                && status == null);
    }
}
