package com.hong.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPagingResponseDto {
    private LocalDateTime nextCursor;
    private List<OrderResponseDto> orders;

}
