package com.hong.hotdealservice.domain;

import com.hong.hotdealservice.domain.base.TimeEntity;
import com.hong.hotdealservice.domain.status.ReserveStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "stock_reservation", indexes = {
        @Index(name = "idx_reserved_product_stock", columnList = "product_id, reserved_quantity"),
})
public class StockReservation extends TimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    // UUID 기반 같은 주문에 대한 식별
    @Column(nullable = false)
    private String reservationToken;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer reservedQuantity;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    //RESERVED, CONFIRMED, CANCELED
    private ReserveStatus status;

    private StockReservation(String orderId, String reservationToken, Long productId, Integer reservedQuantity, LocalDateTime expiresAt) {
        this.orderId = orderId;
        this.reservationToken = reservationToken;
        this.productId = productId;
        this.reservedQuantity = reservedQuantity;
        this.expiresAt = expiresAt;
        this.status = ReserveStatus.RESERVED;
    }

    // == 생성 메서드 ==
    public static StockReservation create(String orderId, String reservationToken, Long productId, Integer reservedQuantity, LocalDateTime expiresAt){
        return new StockReservation(orderId, reservationToken, productId, reservedQuantity, expiresAt);
    }

    // == 주문 성공 처리 ==
    public void updateToConfirmed(){
        this.status = ReserveStatus.CONFIRMED;
    }

    // == 주문 실패 처리 ==
    public void updateToCanceled(){
        this.status = ReserveStatus.CANCELED;
    }

    // == 재고 감소 실패 처리 ==
    public void updateToError(){
        this.status = ReserveStatus.ERROR;
    }
}
