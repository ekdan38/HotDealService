package com.hong.orderservice.event;

import com.hong.common.dto.PaymentCancelRequestDto;
import com.hong.common.dto.PaymentCancelResponseDto;
import com.hong.common.dto.PaymentCreateRequestDto;
import com.hong.common.dto.PaymentCreateResponseDto;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.client.payment.PaymentServiceClient;
import com.hong.orderservice.domain.outbox.CancelPaymentOutbox;
import com.hong.orderservice.repository.outbox.CancelPaymentOutboxRepository;
import com.hong.orderservice.repository.outbox.CreatePaymentOutboxRepository;
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
@Slf4j(topic = "[CancelPaymentEventListener]")
public class CancelPaymentEventListener {
    private final PaymentServiceClient paymentServiceClient;
    private final CancelPaymentOutboxRepository outboxRepository;

    @Async("EventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCancelPaymentOutbox(CancelPaymentOutbox outbox) {

        Long userId = outbox.getUserId();
        String orderId = outbox.getOrderId();

        try {
            // 1. FeignClient RequestDto 생성
            PaymentCancelRequestDto requestDto = new PaymentCancelRequestDto(orderId, userId);

            // 2. FeignClient 호출
            PaymentCancelResponseDto responseDto = paymentServiceClient.cancelPayment(requestDto);

            // 3. Payment 취소 완료. Outbox Status = sent
            if (responseDto.isSuccess()) {
                log.info("Payment 취소 완료. userId = {}, orderId = {}", userId, orderId);
                outbox.updateToSent();
            }
            // 4. Payment 취소 실패.(circuitBreaker, Retry Fallback -> 재시도 대상)
            else if(responseDto.isRetriable()){
                log.error("Payment 취소 실패. (CircuitBreaker OR Retry Fallback 발생). userId = {}, orderId = {}", userId, orderId);
                outbox.updateToFailed();
            }
        }
        // 5. OrderException 이면, 비지니스 로직상 의도된 예외이기 때문에 Outbox Status = skip 처리,
        // 다시 예외 던져서 GlobalHandler 로 예외 응답 처리
        catch (OrderException e){
            log.error("Payment 취소 실패. 비지니스 로직상 예외 발생. userId = {}, orderId = {}", userId, orderId);
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

        // 7. 상태 변경된 Outbox 저장
        outboxRepository.save(outbox);
    }
}
