package com.hong.hotdeal.domain;

import com.hong.hotdeal.domain.base.TimeEntity;
import com.hong.hotdeal.domain.status.DeliveryStatus;
import com.hong.hotdeal.domain.status.OrderStatus;
import com.hong.hotdeal.exception.ErrorCode;
import com.hong.hotdeal.exception.OrderException;
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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProduct> orderProducts = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private Order(User user, Delivery delivery) {
        this.user = user;
        this.delivery = delivery;
        this.status = OrderStatus.COMPLETE;
    }

    // == 생성 메서드 ==
    public static Order create(User user, Delivery delivery, List<OrderProduct> orderProducts){
        Order order = new Order(user, delivery);
        for (OrderProduct orderProduct : orderProducts) {
            order.addOrderProduct(orderProduct);
        }
        return order;
    }

    // == orderProduct와 연관 관계 메서드 ==
    public void addOrderProduct(OrderProduct orderProduct) {
        this.orderProducts.add(orderProduct);
        orderProduct.setOrder(this); // 연관 관계 주입
    }

    // == 주문 취소 메서드 ==
    public void cancel(){
        if(this.delivery.getStatus() == DeliveryStatus.DELIVERED){
            throw new OrderException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }
        this.status = OrderStatus.CANCEL;
        this.delivery.updateStatus(DeliveryStatus.CANCEL);
        for (OrderProduct orderProduct : orderProducts) {
            orderProduct.cancel();
        }
    }

    // == 주문 총 가격 ==
    public int getTotalPrice(){
        int totalPrice = 0;
        for (OrderProduct orderProduct : orderProducts) {
            totalPrice += orderProduct.getTotalPrice();
        }
        return totalPrice;
    }

}
