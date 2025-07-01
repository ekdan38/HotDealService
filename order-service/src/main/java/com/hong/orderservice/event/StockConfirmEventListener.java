package com.hong.orderservice.event;

import com.hong.common.dto.StockFinalizeRequestDto;
import com.hong.common.dto.StockFinalizeResponseDto;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.client.hotdeal.Resilience4JHotDealServiceClient;
import com.hong.orderservice.domain.outbox.StockConfirmOutbox;
import com.hong.orderservice.domain.status.OrderStatus;
import com.hong.orderservice.repository.outbox.StockConfirmOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Slf4j(topic = "[StockConfirmEventListener]")
public class StockConfirmEventListener {

    private final Resilience4JHotDealServiceClient hotDealServiceClient;
    private final StockConfirmOutboxRepository outboxEventRepository;

    // order Status 변경 후 hotdealService Feign 호출 이벤트
    @Async("EventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockConfirmOutboxEvent(StockConfirmOutbox outbox) {

        Long userId = outbox.getUserId();
        String orderId = outbox.getOrderId();
        OrderStatus orderStatus = outbox.getOrderStatus();

        try{
            // 1. OrderStatus 에 따라 재고 반영
            boolean orderSuccess = orderStatus.equals(OrderStatus.PAID);

            // 2. FeignClient RequestDto 생성
            StockFinalizeRequestDto requestDto = new StockFinalizeRequestDto(orderId, orderSuccess);

            // 3. FeignClient 호출
            StockFinalizeResponseDto responseDto = hotDealServiceClient.finalizeStockReservation(requestDto);

            // 4. 재고 최종 반영 성공이면, Outbox Status = sent
            if(responseDto.isSuccess()) {
                log.info("재고 최종 반영 성공. userId = {}, orderId = {}", userId, orderId);
                outbox.updateToSent();
            }
            // 5. 재고 최종 반영 실패(circuitBreaker, Retry Fallback -> 재시도 대상)
            else if(responseDto.isRetriable()){
                log.error("재고 최종 반영 실패(CircuitBreaker OR Retry Fallback 발생). userId = {}, orderId = {}", userId, orderId);
                outbox.updateToFailed();
            }
        }
        // 6. OrderException 이면, 비지니스 로직상 의도된 예외이기 때문에 Outbox Status = skip 처리,
        // 다시 예외 던져서 GlobalHandler 로 예외 응답 처리
        catch (OrderException e){
            log.error("재고 최종 반영 실패. 비지니스 로직상 예외 발생. userId = {}, orderId = {}", userId, orderId);
            outbox.updateToSkip();
            // 다시 예외 던져서 GlobalHandler 로 예외 응답 처리
            throw e;
        }
        // 7. 예기치 못한 예외
        catch (Exception e){
            log.error("알 수 없는 오류 발생. userId = {}, orderId = {}, errorMessage = {}",
                    userId, orderId, e.getMessage());
            outbox.updateToFailed();
        }

        // 8. 상태 변경된 Outbox 저장
        outboxEventRepository.save(outbox);
    }
}
