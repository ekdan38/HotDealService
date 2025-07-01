package com.hong.orderservice.event;

import com.hong.common.dto.UserCartDeleteRequestDto;
import com.hong.common.dto.UserCartDeleteResponseDto;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.client.user.Resilience4JUserServiceClient;
import com.hong.orderservice.domain.outbox.UserCartOutbox;
import com.hong.orderservice.dto.OrderProductIdDto;
import com.hong.orderservice.repository.OrderRepository;
import com.hong.orderservice.repository.outbox.UserCartOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Slf4j(topic = "[UserCartEventListener]")
public class UserCartEventListener {

    private final Resilience4JUserServiceClient userServiceClient;
    private final UserCartOutboxEventRepository userCartOutboxEventRepository;
    private final OrderRepository orderRepository;

    // order 주문 처리 완료 시 user Cart 정리 요청
    @Async("EventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCartOutbox(UserCartOutbox outbox) {

        Long userId = outbox.getUserId();
        String orderId = outbox.getOrderId();

        try{
            // 1. 주문 한 productIds 조회
            List<OrderProductIdDto> foundProductIds = orderRepository.findProductIdsByIdAndUserId(orderId, userId);
            List<Long> productIds = foundProductIds.stream()
                    .map(OrderProductIdDto::getProductId)
                    .toList();

            // 2. FeignClient RequestDto 생성
            UserCartDeleteRequestDto requestDto = new UserCartDeleteRequestDto(userId, productIds);

            // 3. FeignClient 호출
            UserCartDeleteResponseDto responseDto = userServiceClient.deleteUserCart(requestDto);

            // 4. UserCart 정리 성공이면, Outbox Status = sent
            if(responseDto.isSuccess()) {
                log.info("유저 장바구니 주문 완료 건 삭제 성공. userId = {}, orderId = {}", userId, orderId);
                outbox.updateToSent();
            }
            // 5. UserCart 정리 실패(CircuitBreaker, Retry Fallback -> 재시도 대상)
            else if(responseDto.isRetriable()){
                log.info("유저 장바구니 주문 완료 건 삭제 실패. (CircuitBreaker OR Retry Fallback 발생). userId = {}, orderId = {}"
                        , userId, orderId);
                outbox.updateToFailed();
            }
        }
        // 6. OrderException 이면, 비지니스 로직상 의도된 예외. Outbox Status = skip
        // 다시 예외 던져서 GlobalHandler 처리
        catch (OrderException e){
            log.error("유저 장바구니 주문 완료 건 삭제 실패. 비지니스 로직상 예외 발생. userId = {}, orderId = {}", userId, orderId);
            outbox.updateToSkip();
            throw e;
        }
        // 7. 예기치 못한 예외
        catch (Exception e){
            log.info("알 수 없는 오류 발생. userId = {}, orderId = {}, errorMessage = {}",
                    userId, orderId, e.getMessage());
            outbox.updateToFailed();
        }

        // 8. 상태 변경된 outbox 저장
        userCartOutboxEventRepository.save(outbox);
    }
}
