package com.hong.hotdeal.service;

import com.hong.hotdeal.repository.DeliveryRepository;
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

    //10 분마다 실행
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void updateDeliveryStatus(){
        // 시간 저장
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime oneDayAgo = now.minusDays(1);
        LocalDateTime twoDaysAgo = now.minusDays(2);

        // D+1 배송 상태 변경
        deliveryRepository.updateStatusToDelivering(oneDayAgo);
        log.info("D+1 배송 상태 변경");

        // D+2 배송 상태 변경
        deliveryRepository.updateStatusToShipped(twoDaysAgo);
        log.info("D+2 배송 상태 변경");
    }
}
