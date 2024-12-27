package com.hong.hotdeal.service;

import com.hong.hotdeal.domain.Delivery;
import com.hong.hotdeal.domain.Order;
import com.hong.hotdeal.domain.status.DeliveryStatus;
import com.hong.hotdeal.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "[ReturnScheduler]")
public class ReturnScheduler {
    private final DeliveryRepository deliveryRepository;

    //10 분마다 실행
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void updateReturnStatus(){
        // 시간 저장
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);

        // D+1 배송 상태 변경
        List<Delivery> returns = deliveryRepository.findAllWithOrderAndProductsByStatusAndUpdatedAtBefore(oneDayAgo);

        // 재고 반영
        for (Delivery delivery : returns) {
            Order order = delivery.getOrder();
            order.getOrderProducts().forEach(op -> {
                op.getProduct().addStock(op.getQuantity());
            });

            delivery.updateStatus(DeliveryStatus.RETURNED);
        }

        log.info("반품 D+1 재고 상태 변경");
    }
}
