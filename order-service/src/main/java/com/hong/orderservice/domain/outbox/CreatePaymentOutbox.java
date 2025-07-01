package com.hong.orderservice.domain.outbox;

import com.hong.orderservice.domain.base.TimeEntity;
import com.hong.orderservice.domain.status.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "create_payment_outbox",
        indexes = {@Index(name = "idx_outbox_status", columnList = "outbox_status")})
public class CreatePaymentOutbox extends TimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Enumerated(value = EnumType.STRING)
    private OutboxStatus outboxStatus;

    @Column(nullable = false)
    private Integer tryCount;

    private CreatePaymentOutbox(String orderId, Long userId, BigDecimal amount, LocalDateTime expireAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.expireAt = expireAt;
        this.outboxStatus = OutboxStatus.PENDING;
        this.tryCount = 0;
    }

    // == 생성 메서드 ==
    public static CreatePaymentOutbox create(String orderId, Long userId, BigDecimal amount, LocalDateTime expireAt) {
        return new CreatePaymentOutbox(orderId, userId, amount, expireAt);
    }

    // == 이벤트 성공 처리 ==
    public void updateToSent(){
        this.outboxStatus = OutboxStatus.SENT;
    }

    // == 이벤트 실패 처리 ==
    public void updateToFailed(){
        this.outboxStatus = OutboxStatus.FAILED;
    }

    // == 이벤트 재시도 처리 스킵 처리 ==
    public void updateToSkip(){
        this.outboxStatus = OutboxStatus.SKIP;
    }

    // == 이벤트 진행중 처리 ==
    public void updateToInProgress(){
        this.outboxStatus = OutboxStatus.IN_PROGRESS;
        this.tryCount++;
    }

}
