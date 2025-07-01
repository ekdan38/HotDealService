package com.hong.paymentservice.event;

import com.hong.common.dto.OrderUpdateRequestDto;
import com.hong.common.dto.OrderUpdateResponseDto;
import com.hong.common.exception.custom.PaymentException;
import com.hong.paymentservice.client.Resilience4JOrderServiceClient;
import com.hong.paymentservice.domain.outbox.OrderStatusOutbox;
import com.hong.paymentservice.domain.status.PaymentStatus;
import com.hong.paymentservice.repository.OrderStatusOutboxRepository;
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
@Slf4j(topic = "[OrderStatusOutboxEventListener]")
public class OrderStatusOutboxEventListener {

    private final Resilience4JOrderServiceClient orderServiceClient;
    private final OrderStatusOutboxRepository outboxEventRepository;

    // 결제 처리 후 성공 실패 여부로 orderService Feign 호출 이벤트
    @Async("EventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOutboxEvent(OrderStatusOutbox outbox) {
        Long userId = outbox.getUserId();
        String orderId = outbox.getOrderId();
        PaymentStatus status = outbox.getPaymentStatus();

        try{
            // 1. PaymentStatus 에 따라 OrderStatus 반영
            boolean paymentSuccess = status.equals(PaymentStatus.COMPLETED);

            // 2. FeignClient RequestDto 생성
            OrderUpdateRequestDto requestDto = new OrderUpdateRequestDto(userId, orderId, paymentSuccess);

            // 3. FeignClient 호출
            OrderUpdateResponseDto responseDto = orderServiceClient.updateOrderStatus(requestDto);

            // 4. Order Status 변경 성공이면, Outbox Status = sent
            if(responseDto.isSuccess()) {
                log.info("주문 상태 업데이트 성공. userId = {}, orderId = {}", userId, orderId);
                outbox.updateToSent();
            }
            // 5. Order Status 변경 실패.(circuitBreaker, Retry Fallback -> 재시도 대상)
            else if(responseDto.isRetriable()){
                log.error("주문 상태 업데이트 실패. (CircuitBreaker OR Retry Fallback 발생) userId = {}, orderId = {}", userId, orderId);
                outbox.updateToFailed();
            }
        }
        // 6. PaymentException 이면, 비지니스 로직상 의도된 예외이기 때문에 Outbox Status = skip 처리,
        // 다시 예외 던져서 GlobalHandler 로 예외 응답 처리
        catch (PaymentException e){
            log.error("주문 상태 업데이트 실패. 비지니스 로직상 예외 발생. userId = {}, orderId = {}", userId, orderId);
            outbox.updateToSkip();
            // 다시 예외 던져서 GlobalHandler 로 예외 응답 처리
            throw e;
        }
        // 6. 예기치 못한 예외
        catch (Exception e){
            log.error("알 수 없는 오류 발생. userId = {}, orderId = {}, errorMessage = {}",
                    userId, orderId, e.getMessage());
            outbox.updateToFailed();
        }
        outboxEventRepository.save(outbox);
    }
}
