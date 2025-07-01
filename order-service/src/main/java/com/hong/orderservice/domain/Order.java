package com.hong.orderservice.domain;

import com.hong.orderservice.domain.base.TimeEntity;
import com.hong.orderservice.domain.status.DeliveryStatus;
import com.hong.orderservice.domain.status.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "orders", indexes = @Index(name = "idx_user_id", columnList = "user_id"))
public class Order extends TimeEntity {

    @Id
    @Column(name = "order_id", updatable = false, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProduct> orderProducts = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal amount;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = true)
    private LocalDateTime paidAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private Order(String id, Long userId) {
        this.id = id;
        this.userId = userId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.expiresAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusMinutes(15);
    }

    // == 생성 메서드 ==
    public static Order create(String id, Long userId, Delivery delivery, List<OrderProduct> orderProducts, BigDecimal amount){
        Order order = new Order(id, userId);
        order.addOrderProducts(orderProducts);
        order.amount = amount;
        order.setDelivery(delivery);
        return order;
    }

    // == orderProduct 와 연관 관계 메서드 ==
    public void addOrderProducts(List<OrderProduct> orderProducts) {
        orderProducts.forEach(this::addOrderProduct);
    }

    private void addOrderProduct(OrderProduct orderProduct){
        this.orderProducts.add(orderProduct);
        orderProduct.setOrder(this);
    }

    // == Delivery 와 연관 관계 메서드 ==
    private void setDelivery(Delivery delivery){
        this.delivery = delivery;
        delivery.setOrder(this);
    }

    // == 주문 만료 메서드 ==
    public void updateToExpired(){
        this.status = OrderStatus.EXPIRED;
        this.delivery.updateToExpired();
    }

    // 환불 처리 메서드
    public void updateToCancel(){
        this.status = OrderStatus.CANCEL;
        this.delivery.updateToCancel();
    }

    // == 반품 메서드 ==
    public void updateToRequestRefund(LocalDateTime startTime){
        this.status = OrderStatus.RETURN_REQUESTED;
        this.delivery.updateToReturnRequested(startTime);
    }

    // == 반품 완료 메서드 ==
    public void updateStatusReturned(LocalDateTime endTime){
        this.status = OrderStatus.RETURNED;
        this.delivery.updateToReturned(endTime);
    }

    // == 결제 성공 적용 메서드 ==
    public void updateToPaymentSuccess(LocalDateTime paidAt){
        this.status = OrderStatus.PAID;
        this.paidAt = paidAt;
        this.delivery.updateStatus(DeliveryStatus.DELIVERABLE);
    }

    // == 결제 실패 적용 메서드 ==
    public void updateToPaymentFailed(){
        this.status = OrderStatus.PAYMENT_FAILED;
    }
}