package com.hong.orderservice.event;

import com.hong.common.dto.PaymentCreateRequestDto;
import com.hong.common.dto.PaymentCreateResponseDto;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.client.payment.PaymentServiceClient;
import com.hong.orderservice.domain.outbox.CreatePaymentOutbox;
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
@Slf4j(topic = "[CreatePaymentEventListener]")
public class CreatePaymentEventListener {
    private final PaymentServiceClient paymentServiceClient;
    private final CreatePaymentOutboxRepository outboxRepository;

    @Async("EventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreatePaymentOutbox(CreatePaymentOutbox outbox) {

        Long userId = outbox.getUserId();
        String orderId = outbox.getOrderId();

        try {
            // 1. FeignClient RequestDto 생성
            PaymentCreateRequestDto requestDto =
                    new PaymentCreateRequestDto(orderId, userId, outbox.getAmount(), outbox.getExpireAt());

            // 2. FeignClient 호출
            PaymentCreateResponseDto responseDto = paymentServiceClient.createPayment(requestDto);

            // 3. Payment 생성 성공이면, Outbox Status = sent
            if (responseDto.isSuccess()) {
                log.info("Payment 생성 완료. userId = {}, orderId = {}", userId, orderId);
                outbox.updateToSent();
            }
            // 4. Payment 생성 실패(circuitBreaker, Retry Fallback -> 재시도 대상)
            else if(responseDto.isRetriable()){
                log.error("Payment 생성 실패. (CircuitBreaker OR Retry Fallback 발생). userId = {}, orderId = {}", userId, orderId);
                outbox.updateToFailed();
            }
        }
        // 5. OrderException 이면, 비지니스 로직상 의도된 예외이기 때문에 Outbox Status = skip 처리,
        // 다시 예외 던져서 GlobalHandler 로 예외 응답 처리
        catch (OrderException e){
            log.error("Payment 생성 실패. 비지니스 로직상 예외 발생. userId = {}, orderId = {}", userId, orderId);
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
