package com.hong.orderservice.scheduler;

import com.hong.orderservice.domain.Order;
import com.hong.orderservice.repository.OrderRepository;
import com.hong.orderservice.service.RefundOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "[RefundScheduler]")
public class RefundScheduler {

    private final OrderRepository orderRepository;
    private final RefundOrderService refundOrderService;

    // 4 시간 마다 실행
    @Scheduled(fixedRate = 1000 * 60 * 60 * 4)
    @Transactional
    @SchedulerLock(
            name = "RefundScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "1m")
    public void processRefund(){
        log.info("환불 처리 요청 후 1일 지난 주문 처리 스케줄러 시작");
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MILLIS);
        // 1. 환불 처리 요청 후 1일 지난 주문 조회
        List<Order> refundedOrders = orderRepository.findRefundedOrders(oneDayAgo);


        // 2. 처리할 환불 존재하지 않으면 return
        if(refundedOrders.isEmpty()) return;

        log.info("환불 처리 요청 후 1이 지난 주문 {}건 조회 완료. 처리 시작.", refundedOrders.size());

        // 3. 환불 처리
        for (Order order : refundedOrders) {
            refundOrderService.processSingleRefundOrder(order);
        }
        log.info("환불 처리 요청 후 1일 지난 주문 처리 스케줄러 종료");
    }
}
