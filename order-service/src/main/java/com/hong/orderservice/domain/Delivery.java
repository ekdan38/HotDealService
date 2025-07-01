package com.hong.orderservice.domain;

import com.hong.orderservice.domain.base.Address;
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

    // == 배송 상태 DELIVERING 으로 update 메서드 ==
    public void updateToDelivering(LocalDateTime startTime){
        this.startedAt = startTime;
        this.status = DeliveryStatus.DELIVERING;
    }

    // == 배송 상태 DELIVERED 으로 update 메서드 ==
    public void updateToDelivered(LocalDateTime completedTime){
        this.completedAt = completedTime;
        this.status = DeliveryStatus.DELIVERED;
    }

    // == 배송 상태 CANCEL 로 update 메서드 ==
    public void updateToCancel(){
        this.status = DeliveryStatus.CANCEL;
    }

    // == 배송 상태 RETURN_REQUESTED 으로 update 메서드 ==
    public void updateToReturnRequested(LocalDateTime startTime){
        this.returnStartedAt = startTime;
        this.status = DeliveryStatus.RETURN_REQUESTED;
    }

    // == 배송 상태 RETURNED 으로 update 메서드 ==
    public void updateToReturned(LocalDateTime endTime){
        this.returnCompletedAt = endTime;
        this.status = DeliveryStatus.RETURNED;
    }

    // == 배송 상태 update 메서드 ==
    public void updateStatus(DeliveryStatus status){
        this.status = status;
    }

    // == 만료된 주문에 의한 처리 메서드 ==
    public void updateToExpired(){
        this.status = DeliveryStatus.EXPIRED;
    }

    // == 배송 시작 날짜 update 메서드 ==
    public void updateStartedAt(LocalDateTime startedAt){
        this.startedAt = startedAt;
    }

    // == returnStartedAt update 메서드 ==
    public void updateReturnStartedAt(LocalDateTime returnStartedAt){
        this.returnStartedAt = returnStartedAt;
    }
}
