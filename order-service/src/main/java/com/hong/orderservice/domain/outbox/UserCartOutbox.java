package com.hong.orderservice.domain.outbox;

import com.hong.orderservice.domain.base.TimeEntity;
import com.hong.orderservice.domain.status.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "user_cart_outbox",
        indexes = {@Index(name = "idx_outbox_status", columnList = "outbox_status")})
public class UserCartOutbox extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "order_id")
    private String orderId;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private OutboxStatus outboxStatus;

    @Column(nullable = false)
    private Integer tryCount;

    private UserCartOutbox(String orderId, Long userId) {
        this.orderId = orderId;
        this.userId = userId;
        this.outboxStatus = OutboxStatus.PENDING;
        this.tryCount = 0;
    }

    // == 생성 메서드 ==
    public static UserCartOutbox create(String orderId, Long userId) {
        return new UserCartOutbox(orderId, userId);
    }

    // == 이벤트 성공 처리 ==
    public void updateToSent(){
        this.outboxStatus = OutboxStatus.SENT;
    }

    // == 이벤트 실패 처리 ==
    public void updateToFailed(){
        this.outboxStatus = OutboxStatus.FAILED;
    }

    // == 이벤트 재시도 처리 스킵 처리 메서드 ==
    public void updateToSkip(){
        this.outboxStatus = OutboxStatus.SKIP;
    }

    // == 이벤트 진행중 처리 메서드 ==
    public void updateToInProgress(){
        this.outboxStatus = OutboxStatus.IN_PROGRESS;
        this.tryCount++;
    }
}
