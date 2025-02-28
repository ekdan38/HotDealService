package com.hong.hotdealservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HotDealProductCacheDto {
    private Long hotDealId;
    private Long hotDealProductId;
    private Long originalProductId;
    private String productTitle;
    private Integer originalPrice;
    private Integer hotDealPrice;
    private Double discountRate;
}
