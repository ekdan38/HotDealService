package com.hong.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.kafka.PaymentResultEventDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.PaymentException;
import com.hong.common.exception.custom.PaymentSessionException;
import com.hong.common.status.AggregateType;
import com.hong.common.status.EventType;
import com.hong.common.status.OutboxDeliveryMethod;
import com.hong.paymentservice.domain.Outbox;
import com.hong.paymentservice.domain.Payment;
import com.hong.paymentservice.domain.PaymentSession;
import com.hong.paymentservice.domain.status.PaymentSessionStatus;
import com.hong.paymentservice.dto.PaymentPerformResponseDto;
import com.hong.paymentservice.dto.PaymentPrepareResponseDto;
import com.hong.paymentservice.event.OutboxEvent;
import com.hong.paymentservice.event.OutboxService;
import com.hong.paymentservice.repository.PaymentRepository;
import com.hong.paymentservice.repository.PaymentSessionRepository;
import com.hong.paymentservice.web.dto.PaymentPerformRequestDto;
import com.hong.paymentservice.web.dto.PaymentPrepareRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[PaymentServiceImpl]")
public class PaymentServiceImpl implements PaymentService {

    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentSessionRepository paymentSessionRepository;
    private final PaymentRepository paymentRepository;

    // 결제 진입
    @Transactional
    @Override
    public PaymentPrepareResponseDto paymentPrepare(Long userId, PaymentPrepareRequestDto requestDto) {
        // 1. orderId 기준 이미 생성된 paymentSession 존재 검증 및 상태 검증
        validateDuplicatePaymentSession(userId, requestDto);

        // 2. payment 조회 및 검증, IN_PROGRESS 변경
        Payment payment = fetchPaymentAndValidateAndInProgress(userId, requestDto);

        // 3. paymentSession 생성
        PaymentSession session = createPaymentSessionAndSave(userId, requestDto, payment.getExpireAt());

        // 4. 응답 Dto 변환
        return convertToPrepareResponse(session.getId());
    }

    // 결제 수행
    @Transactional
    @Override
    public PaymentPerformResponseDto performPayment(Long userId, PaymentPerformRequestDto requestDto) {
        // 1. paymentSession 조회 및 존재 검증
        PaymentSession session = getSessionAndValidate(userId, requestDto);

        // 2. payment 조회 및 존재 검증
        Payment payment = getPaymentAndValidate(userId, session);

        // 4. pg 사 결제 시뮬레이션 (80% 성공)
        boolean paymentResult = simulatePayment(payment.getExpireAt(), requestDto.getAmount(), session);

        // 결제 처리
        // 결제 성공 처리
        if(paymentResult){
            session.updateStatusToSuccess();
            payment.updateToSuccess(session.getId());
        }
        // 결제 실패 처리
        else {
            session.updateStatusToFail();
            payment.updateToFail(session.getId());
        }
        // 5. outbox 생성 및 save
        Outbox outbox = saveOutbox(payment.getId(), payment.getOrderId(), userId, payment.getPaidAt(), paymentResult, OutboxDeliveryMethod.KAFKA);

        // 6. outbox 이벤트 발행
        eventPublisher.publishEvent(new OutboxEvent(outbox));

        // 7. 응답 Dto 반환
        return new PaymentPerformResponseDto(session.getStatus().name(), payment.getTransactionId());
    }

    private Outbox saveOutbox(Long paymentId, String orderId, Long userId, LocalDateTime paidAt, boolean paymentResult, OutboxDeliveryMethod deliveryMethod){
        try{
            PaymentResultEventDto payloadDto = new PaymentResultEventDto(orderId, userId, paymentResult, paidAt);
            String payload = objectMapper.writeValueAsString(payloadDto);
            return outboxService.save(Outbox.create(AggregateType.PAYMENT, paymentId, EventType.PAYMENT_RESULT, deliveryMethod, payload));

        }catch (Exception e){
            log.error("결제 처리 중 오류 발생. errorMessage = {}", e.getMessage());
            throw new PaymentException(ErrorCode.PAYMENT_INTERNAL_SERVER_ERROR);
        }
    }

    private Payment getPaymentAndValidate(Long userId, PaymentSession session) {
       return paymentRepository.findByOrderIdAndUserId(session.getOrderId(), userId).orElseThrow(() -> {
            log.error("존재 하지 않는 결제입니다. userId = {}, orderId = {}, paymentId = {}", session.getOrderId(), userId, null);
            return new PaymentException(ErrorCode.PAYMENT_NOT_FOUND, userId, session.getOrderId(), null);
        });
    }
    private Payment fetchPaymentAndValidateAndInProgress(Long userId, PaymentPrepareRequestDto requestDto) {
        Payment payment = paymentRepository.findByOrderIdAndUserId(requestDto.getOrderId(), userId).orElseThrow(() -> {
            log.error("결제가 준비되지 않았습니다. 잠시 후 다시 시도해주세요. userI = {}, orderId = {}", userId, requestDto.getOrderId());
            return new PaymentException(ErrorCode.PAYMENT_NOT_READY, userId, requestDto.getOrderId());
        });
        payment.updateToInProgress();
        return payment;
    }

    private PaymentSession getSessionAndValidate(Long userId, PaymentPerformRequestDto requestDto) {
        PaymentSession session = paymentSessionRepository.findByIdAndUserId(requestDto.getSessionId(), userId)
                .orElseThrow(() -> {
                    log.error("결제 세션이 존재하지 않습니다. userId = {}, sessionId = {}", userId, requestDto.getSessionId());
                    return new PaymentSessionException(ErrorCode.PAYMENT_SESSION_NOT_FOUND, userId, requestDto.getSessionId());
                });
        if (!session.getStatus().equals(PaymentSessionStatus.READY)) {
            log.error("결제 수행 가능한 결제 세션이 아닙니다. userId = {}, sessionId = {}", userId, requestDto.getSessionId());
            throw new PaymentSessionException(ErrorCode.PAYMENT_SESSION_INVALID_STATUS, userId, requestDto.getSessionId());
        }
        return session;
    }

    private void validateDuplicatePaymentSession(Long userId, PaymentPrepareRequestDto requestDto) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        paymentSessionRepository.findByOrderIdAndUserId(requestDto.getOrderId(), userId)
                .ifPresent(existing -> {
                    if (existing.getExpireAt().isAfter(now) && existing.getStatus().equals(PaymentSessionStatus.READY)) {
                        log.error("이미 진행 중인 결제 세션이 존재합니다. userId = {}, sessionId = {}", userId, existing.getId());
                        throw new PaymentSessionException(ErrorCode.PAYMENT_SESSION_ALREADY_EXISTS, userId, existing.getId());
                    }
                });
    }

    private boolean simulatePayment(LocalDateTime expireAt, BigDecimal amount, PaymentSession paymentSession){
        // 금액 확인
        if (amount.compareTo(paymentSession.getAmount()) < 0) {
            log.warn("결제 금액 부족. expected = {}, actual = {}", paymentSession.getAmount(), amount);
            return false;
        }
        // 결제 완료 시점 expireAt 확인
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        return !now.isAfter(expireAt);
    }

    private PaymentSession createPaymentSessionAndSave(Long userId, PaymentPrepareRequestDto requestDto, LocalDateTime expireAt) {
        PaymentSession paymentSession = PaymentSession.create(requestDto.getOrderId(), userId, requestDto.getAmount(), expireAt);
        paymentSessionRepository.save(paymentSession);
        return paymentSession;
    }

    private PaymentPrepareResponseDto convertToPrepareResponse(String sessionId) {
        return new PaymentPrepareResponseDto(sessionId, "/payments/progress?sessionId=" + sessionId);
    }
}
