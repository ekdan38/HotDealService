package com.hong.orderservice.domain;

import com.hong.common.entity.TimeEntity;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.domain.status.DeliveryStatus;
import com.hong.orderservice.domain.status.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "orders")
public class Order extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProduct> orderProducts = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private Order(Long userId) {
        this.userId = userId;
        this.status = OrderStatus.PENDING;
    }

    // == 생성 메서드 ==
    public static Order create(Long userId, Delivery delivery, List<OrderProduct> orderProducts){
        Order order = new Order(userId);
        order.addOrderProducts(orderProducts);
        order.setDelivery(delivery);
        return order;
    }

    // == orderProduct와 연관 관계 메서드 ==
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

    // == 주문의 총 가격 반환 메서드 ==
    public Integer getTotalPrice() {
        int totalPrice = 0;
        for (OrderProduct orderProduct : orderProducts) {
            totalPrice += orderProduct.getTotalPrice();
        }
        return totalPrice;
    }

    // == 주문 취소 메서드 ==
    public void cancel() {
        this.status = OrderStatus.CANCEL;
        this.delivery.updateStatus(DeliveryStatus.CANCEL);
    }

    // == 반품 메서드 ==
    public void returnOrder(){
        this.status = OrderStatus.RETURN_REQUESTED;
        this.delivery.updateStatus(DeliveryStatus.RETURN_REQUESTED);
    }
}