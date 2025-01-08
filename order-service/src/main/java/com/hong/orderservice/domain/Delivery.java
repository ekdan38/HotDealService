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

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(updatable = false)
    private LocalDateTime completedAt;

    @Column(updatable = false)
    private LocalDateTime returnStartedAt;

    @Column(updatable = false)
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
    protected void updateStatus(DeliveryStatus status){
        this.status = status;
    }
}
