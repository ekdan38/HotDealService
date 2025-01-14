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
    @Scheduled(fixedRate = 14400000 )
    @Transactional
    public void updateDeliveryStatus(){
        // 시간 저장
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime oneDayAgo = now.minusDays(1);
        LocalDateTime twoDaysAgo = now.minusDays(2);

        // D+1 배송 상태 변경
        deliveryRepository.updateOrderStatus(oneDayAgo, DeliveryStatus.DELIVERING, DeliveryStatus.PENDING);
        log.info("D+1 배송 상태 변경");

        // D+2 배송 상태 변경
        deliveryRepository.updateOrderStatus(twoDaysAgo, DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERING);
        log.info("D+2 배송 상태 변경");
    }
}
