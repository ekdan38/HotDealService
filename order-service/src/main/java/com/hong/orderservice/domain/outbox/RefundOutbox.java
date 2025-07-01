package com.hong.orderservice.domain.outbox;

import com.hong.orderservice.domain.base.TimeEntity;
import com.hong.orderservice.domain.status.OutboxStatus;
import com.hong.orderservice.domain.status.RefundOutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RefundOutbox extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "order_id")
    private String orderId;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private RefundOutboxStatus refundOutboxStatus;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private OutboxStatus outboxStatus;

    private RefundOutbox(String orderId, Long userId, RefundOutboxStatus refundOutboxStatus, OutboxStatus outboxStatus) {
        this.orderId = orderId;
        this.userId = userId;
        this.refundOutboxStatus = refundOutboxStatus;
        this.outboxStatus = outboxStatus;
    }

    public static RefundOutbox create(String orderId, Long userId, RefundOutboxStatus refundOutboxStatus, OutboxStatus outboxStatus) {
        return new RefundOutbox(orderId, userId, refundOutboxStatus, outboxStatus);
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
}
