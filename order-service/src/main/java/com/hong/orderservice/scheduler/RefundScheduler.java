package com.hong.orderservice.scheduler;

import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.outbox.CancelPaymentOutbox;
import com.hong.orderservice.domain.outbox.RestoreStockOutbox;
import com.hong.orderservice.repository.OrderRepository;
import com.hong.orderservice.repository.outbox.CancelPaymentOutboxRepository;
import com.hong.orderservice.repository.outbox.RestoreStockOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "[RefundScheduler]")
public class RefundScheduler {

    private final OrderRepository orderRepository;
    private final RestoreStockOutboxRepository hotDealOutboxRepository;
    private final CancelPaymentOutboxRepository paymentOutboxRepository;

    // 4 시간 마다 실행
    @Scheduled(fixedRate = 1000 * 60 * 60 * 4)
    @Transactional
    @SchedulerLock(
            name = "DeliveryScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "30s")
    public void processRefund(){
        log.info("환불 완료 처리 스케쥴러 시작");
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MILLIS);

        int pageIndex = 0;
        // 100개씩 처리
        int pageSize = 100;
        Page<Order> page;
        do {
            page = orderRepository.findRefundedOrders(oneDayAgo, PageRequest.of(pageIndex, pageSize));

            for (Order order : page.getContent()) {
                processRefund(order);
            }
            pageIndex++;
        }
        while (page.hasNext());
        log.info("환불 완료 처리 스케쥴러 종료");
    }

    private void processRefund(Order order) {
        String orderId = order.getId();
        Long userId = order.getUserId();
        log.info("환불 처리 시작. orderId = {}, userId = {}", orderId, userId);

        // 1. 결제 취소 요청 outbox 생성
        RestoreStockOutbox restoreStockOutbox = RestoreStockOutbox.create(orderId, userId, order.getAmount());
        hotDealOutboxRepository.save(restoreStockOutbox);

        // 2. 재고 restore 요청 outbox 생성
        CancelPaymentOutbox cancelPaymentOutbox = CancelPaymentOutbox.create(orderId, userId, order.getAmount());
        paymentOutboxRepository.save(cancelPaymentOutbox);
    }
}
