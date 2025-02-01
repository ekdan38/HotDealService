package com.hong.orderservice.domain;

import com.hong.common.entity.Address;
import com.hong.orderservice.domain.status.DeliveryStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long id;

    @OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
    private Order order;

    @Embedded
    @Column(nullable = false)
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime startedAt;

    @Column(nullable = true)
    private LocalDateTime completedAt;

    @Column(nullable = true)
    private LocalDateTime returnStartedAt;

    @Column(nullable = true)
    private LocalDateTime returnCompletedAt;

    private Delivery(Address address) {
        this.address = address;
        this.status = DeliveryStatus.PENDING;
    }
    // == 생성 메서드 ==
    public static Delivery create(Address address){
        return new Delivery(address);
    }

    // == order 에서 사용 하는 연관 관계 메서드 ==
    protected void setOrder(Order order){
        this.order = order;
    }

    // == 배송 상태 update 메서드 ==
    public void updateStatus(DeliveryStatus status){
        this.status = status;
    }

    // == 배송 시작 날짜 update 메서드 ==
    public void updateStartedAt(LocalDateTime startedAt){
        this.startedAt = startedAt;
    }
}
