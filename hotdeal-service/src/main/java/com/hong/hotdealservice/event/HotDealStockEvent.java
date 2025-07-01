package com.hong.hotdealservice.event;

import com.hong.hotdealservice.domain.HotDeal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HotDealStockEvent {
    private final HotDeal hotDeal;
}
