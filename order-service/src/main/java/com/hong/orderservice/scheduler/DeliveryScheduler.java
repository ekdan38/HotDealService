package com.hong.orderservice.scheduler;

import com.hong.orderservice.domain.status.DeliveryStatus;
import com.hong.orderservice.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "[DeliveryScheduler]")
public class DeliveryScheduler {

    private final DeliveryRepository deliveryRepository;

    // 4 시간 마다 실행
    @Scheduled(fixedRate = 14400000)
    @Transactional
    public void updateDeliveryStatus(){
        LocalDateTime now = LocalDateTime.now();

        // 주문 후 1일 경과한 배송 DELIVERING 로 상태 변경
        deliveryRepository.updatePendingDeliveriesToDelivering(
                now.minusDays(1),
                DeliveryStatus.DELIVERING,
                LocalDateTime.now(),
                DeliveryStatus.PENDING
        );
        log.info("Scheduler DELIVERING 로 상태 변경");

        // 주문 후 2일 경과한 배송 DELIVERED 로 상태 변경
        deliveryRepository.updatePendingDeliveriesToDelivering(
                now.minusDays(2),
                DeliveryStatus.DELIVERING,
                LocalDateTime.now(),
                DeliveryStatus.DELIVERED
        );
        log.info("Scheduler DELIVERED 로 상태 변경");
    }
}
