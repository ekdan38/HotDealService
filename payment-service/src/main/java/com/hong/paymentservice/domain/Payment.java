package com.hong.paymentservice.domain;

import com.hong.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Payment extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    private Payment(Long orderId, Integer amount) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        expireAt = LocalDateTime.now().plusMinutes(15);
    }

    // == 생성 메서드 ==/
    public static Payment create(Long orderId, Integer amount){
        return new Payment(orderId, amount);
    }

    // == payment 결제 제한 시간 만료 확인 메서드 ==
    public Boolean expiredPay(){
        return expireAt.isBefore(LocalDateTime.now());
    }

    // == payment 결제 상태 변경 메서드 ==
    public void updateStatus(PaymentStatus newStatus){
        this.status = newStatus;
    }
}
