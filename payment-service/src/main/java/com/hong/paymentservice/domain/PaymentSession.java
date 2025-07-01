package com.hong.paymentservice.domain;

import com.hong.paymentservice.domain.base.TimeEntity;
import com.hong.paymentservice.domain.status.PaymentSessionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "payment_session", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),

})
public class PaymentSession extends TimeEntity {

    @Id
    @Column(name = "session_id")
    private String id;

    // UUID
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal amount;

    // READY, SUCCESS, FAIL
    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private PaymentSessionStatus status;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    private PaymentSession(String orderId, Long userId, BigDecimal amount, LocalDateTime expireAt) {
        this.id = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = PaymentSessionStatus.READY;
        this.expireAt = expireAt;
    }

    public static PaymentSession create(String orderId, Long userId, BigDecimal amount, LocalDateTime expireAt){
        return new PaymentSession(orderId, userId, amount, expireAt);
    }

    public void updateStatusToSuccess(){
        this.status = PaymentSessionStatus.SUCCESS;
    }
    public void updateStatusToFail(){
        this.status = PaymentSessionStatus.FAIL;
    }
}
