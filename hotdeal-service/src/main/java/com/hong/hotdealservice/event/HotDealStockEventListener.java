package com.hong.hotdealservice.event;

import com.hong.hotdealservice.service.HotDealStockRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotDealStockEventListener {

    private final HotDealStockRedisService hotDealStockRedisService;

    // Redis 에 stock 저장 Event
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(HotDealStockEvent event){
        event.getHotDeal()
                .getHotDealProducts()
                .forEach(hp -> hotDealStockRedisService.saveStockInRedisWithRetry(hp.getId(), hp.getStock(), event.getHotDeal().getEndTime()));
    }
}
