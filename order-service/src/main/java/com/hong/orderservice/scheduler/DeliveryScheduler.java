package com.hong.orderservice.scheduler;

import com.hong.orderservice.repository.DeliveryRepository;
import com.hong.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "[DeliveryScheduler]")
public class DeliveryScheduler {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;

    // 4 시간 마다 실행
    @Scheduled(fixedRate = 1000 * 60 * 60 * 4)
    @Transactional
    @SchedulerLock(
            name = "DeliveryScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "1m")
    public void updateDeliveryStatus(){
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        // 결제 완료, 주문 후 1일 경과한 배송 DELIVERING 로 상태 변경
        deliveryRepository.bulkUpdatePendingDeliveriesToDelivering(
                now.minusDays(1),
                LocalDateTime.now()
        );
        log.info("Scheduler DELIVERING 로 상태 변경");

        // 결제 완료, 배송 시작 후 1일 경과한 배송 DELIVERED 로 상태 변경
        deliveryRepository.bulkUpdateDeliveringDeliveriesToDelivered(
                now.minusDays(1),
                LocalDateTime.now()
        );
        log.info("Scheduler DELIVERED 로 상태 변경");

        //// 재고 복구 처리 해야됨
        // 배송 상태 환불 완료 처리
        deliveryRepository.bulkUpdateDeliveryStatusReturned(
                now.minusDays(1),
                now
        );
        // 주문 상태 환불 완료 처리
        orderRepository.bulkUpdateOrderStatusToReturned(
                now.minusDays(1)
        );
        log.info("Scheduler 주문, 배송 환불 완료 상태 변경");
    }
}
