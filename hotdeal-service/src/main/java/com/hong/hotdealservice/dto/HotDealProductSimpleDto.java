package com.hong.hotdealservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotDealProductSimpleDto {

    private Long id;
    private Long hotdealId;
    private String title;
    private BigDecimal price;
}
