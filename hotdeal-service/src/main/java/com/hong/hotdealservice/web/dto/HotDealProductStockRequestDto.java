package com.hong.hotdealservice.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotDealProductStockRequestDto {

    private List<Long> hotDealProductIds;
}
