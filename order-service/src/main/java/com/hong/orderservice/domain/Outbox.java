package com.hong.orderservice.domain;

import com.hong.orderservice.domain.base.TimeEntity;
import com.hong.orderservice.domain.status.EventType;
import com.hong.orderservice.domain.status.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outbox",
        indexes = {@Index(name = "idx_outbox_status", columnList = "outbox_status"),
                   @Index(name = "idx_event_type", columnList = "event_type")})
public class Outbox extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Outbox 발행 도메인
    @Column(nullable = false)
    private String aggregateType; // 예: "ORDER", "PAYMENT"

    // 발행 도메인의 PK
    @Column(nullable = false)
    private String aggregateId;   // 예: orderId, paymentId

    // 이벤트 발행 타입
    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private EventType eventType;     // 예: "order.created", "payment.requested"

    // 이벤트 데이터 Json
    @Column(nullable = false)
    private String payload;       // 실제 이벤트 데이터 (JSON String)

    // PENDING, PUBLISHED, FAILED
    @Enumerated(value = EnumType.STRING)
    private OutboxStatus outboxStatus;

    // 재시도 횟수
    @Column(nullable = false)
    private Integer tryCount;

    // 발행 날짜
    private LocalDateTime publishedAt;

    private Outbox(String aggregateId, EventType eventType, String payload) {
        this.aggregateType = "ORDER";
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.outboxStatus= OutboxStatus.PENDING;
        this.tryCount = 0;
        this.publishedAt = null;
    }

    // 생성 메서드
    public static Outbox create(String aggregateId, EventType eventType, String payload) {
        return new Outbox(aggregateId, eventType, payload);
    }

    public void updateToPublished(){
        this.outboxStatus = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public void updateToFailed(){
        this.outboxStatus = OutboxStatus.FAILED;
    }
}
