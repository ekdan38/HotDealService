package com.hong.hotdeal.web.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderPagingResponseDto {
    private Long cursor;
    private List<OrderSummaryResponseDto> orders;
}
