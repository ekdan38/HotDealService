package com.hong.orderservice.scheduler;

import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.outbox.ExpirePaymentOutbox;
import com.hong.orderservice.domain.outbox.ReleaseStockOutbox;
import com.hong.orderservice.repository.OrderRepository;
import com.hong.orderservice.repository.outbox.ExpirePaymentOutboxRepository;
import com.hong.orderservice.repository.outbox.ReleaseStockOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Profile("!test")
@Component
@RequiredArgsConstructor
@Transactional
@Slf4j(topic = "[ExpiredOrderScheduler]")
public class ExpiredOrderScheduler {

    private final OrderRepository orderRepository;
    private final ExpirePaymentOutboxRepository paymentOutboxRepository;
    private final ReleaseStockOutboxRepository hotDealOutboxRepository;

    /**
     * 만료 된 order Expired 처리
     * => hotDealService 점유 재고 삭제
     * => paymentService payment 상태 변경
     */
    @Scheduled(fixedDelay = 60_000) // 1분 주기
    @SchedulerLock(
            name = "ExpiredOrderScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "30s")
    public void cancelExpiredOrders1() {
        // 1. 만료된 주문 조회
        List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        // 2. 만료된 주문 처리
        if (!expiredOrders.isEmpty()) {
            for (Order order : expiredOrders) {
                order.updateToExpired();

                // 3. 재고 점유 해제 outbox 생성
                ReleaseStockOutbox releaseStockOutbox = ReleaseStockOutbox.create(order.getId(), order.getUserId(), order.getAmount());
                hotDealOutboxRepository.save(releaseStockOutbox);

                // 4. 결제 만료 outbox 생성
                ExpirePaymentOutbox expirePaymentOutbox = ExpirePaymentOutbox.create(order.getId(), order.getUserId(), order.getAmount());
                paymentOutboxRepository.save(expirePaymentOutbox);
            }
        }
    }
}

