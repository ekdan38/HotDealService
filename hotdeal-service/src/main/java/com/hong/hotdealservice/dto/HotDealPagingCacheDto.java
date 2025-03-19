package com.hong.hotdealservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotDealPagingCacheDto {

    private Long cursor;
    private List<HotDealCacheDto> hotDeals;
}
