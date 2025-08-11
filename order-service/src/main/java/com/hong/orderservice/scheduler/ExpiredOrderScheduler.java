package com.hong.orderservice.scheduler;

import com.hong.orderservice.domain.Order;
import com.hong.orderservice.repository.OrderRepository;
import com.hong.orderservice.service.ExpiredOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Profile("!test")
@Component
@RequiredArgsConstructor
@Slf4j(topic = "[ExpiredOrderScheduler]")
public class ExpiredOrderScheduler {

    private final OrderRepository orderRepository;
    private final ExpiredOrderService expiredOrderService;

    /**
     * 만료 된 order Expired 처리
     * => hotDealService 재고 점유 해제 Kafka Produce
     * => paymentService payment 상태 변경 Kafka Produce
     */
    @Scheduled(fixedDelay = 1000 * 60 * 60 * 4) // 4시간 주기
    @SchedulerLock(
            name = "ExpiredOrderScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "30s")
    public void cancelExpiredOrders() {
        log.info("만료된 주문 처리 스케줄러 실행");
        // 1. 만료된 주문 조회
        List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        // 2. 처리할 주문 존재하지 않으면 return
        if(expiredOrders.isEmpty()) return;

        log.info("만료된 주문 {}건 조회 완료. 처리 시작.", expiredOrders.size());

        // 3. 만료된 주문 처리
        for (Order order : expiredOrders) {
            expiredOrderService.processSingleExpiredOrder(order);
        }
        log.info("만료되 주문 처리 스케줄러 종료");
    }
}

