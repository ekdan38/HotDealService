package com.hong.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPagingResponseDto {
    private Long nextCursor;
    private List<OrderResponseDto> orderResponseDtos;
}
