package com.hong.hotdeal.domain;

import com.hong.hotdeal.domain.status.DeliveryStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Getter
public class Delivery {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    private LocalDateTime completedAt;

    private Delivery(Address address) {
        this.address = address;
        this.status = DeliveryStatus.PENDING;
    }

    // == 생성 메서드 ==
    public static Delivery create(Address address){
        return new Delivery(address);
    }

    // == 배송 상태 업데이트 메서드 ==
    public void updateStatus(DeliveryStatus status){
        this.status = status;
    }

//    public void startDelivery(){
//        this.startedAt = LocalDateTime.now();
//        this.status = DeliveryStatus.DELIVERING;
//    }
//
//    public void completeDelivery(){
//        this.completedAt = LocalDateTime.now();
//        this.status = DeliveryStatus.SHIPPED;
//    }

}
