package com.hong.paymentservice.domain;

import com.hong.paymentservice.domain.base.TimeEntity;
import com.hong.paymentservice.domain.status.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "payment", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_user_id", columnList = "user_id")
})
public class Payment extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = true)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = true)
    private LocalDateTime paidAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Column(nullable = true)
    private LocalDateTime canceledAt;

    private Payment(String orderId, BigDecimal amount, Long userId, LocalDateTime expireAt) {
        this.orderId = orderId;
        this.amount = amount;
        this.userId = userId;
        this.status = PaymentStatus.PENDING;
        this.expireAt = expireAt;
    }

    // == 생성 메서드 ==/
    public static Payment create(String orderId, BigDecimal amount, Long userId, LocalDateTime expireAt){
        return new Payment(orderId, amount, userId, expireAt);
    }

    // == payment 결제 성공 변경 메서드 ==
    public void updateToSuccess(String transactionId){
        this.transactionId = transactionId;
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    // == payment 결제 실패 변경 메서드 ==
    public void updateToFail(String transactionId){
        this.transactionId = transactionId;
        this.status = PaymentStatus.FAILED;
    }

    // == payment EXPIRED 처리 메서드 ==
    public void updateToExpired(){
        this.status = PaymentStatus.EXPIRED;
    }

    // == payment IN_PROGRESS 처리 메서드 ==
    public void updateToInProgress(){
        this.status = PaymentStatus.IN_PROGRESS;
    }

    // == payment CANCELED 처리 메서드 ==
    public void updateToCancel(){
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
