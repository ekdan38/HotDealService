package com.hong.hotdeal.domain;

import com.hong.hotdeal.domain.base.TimeEntity;
import com.hong.hotdeal.exception.ErrorCode;
import com.hong.hotdeal.exception.OrderException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OrderProduct extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer price;

    private OrderProduct(Product product, Integer quantity, Integer price) {
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    // == 생성 메서드 ==
    public static OrderProduct create(Product product, Integer quantity){
        if(quantity <= 0){
            throw new OrderException(ErrorCode.ORDER_QUANTITY_INVALID);
        }
        if(product.getStock() < quantity){
            throw new OrderException(ErrorCode.ORDER_PRODUCT_NO_STOCK);
        }
        product.removeStock(quantity);
        return new OrderProduct(product, quantity, product.getPrice());
    }

    // == Order 에서 사용할 연관 관계 관련 메서드 ==
    public void setOrder(Order order){
        this.order = order;
    }

    // == 총 가격 == //
    public int getTotalPrice(){
        return quantity * price;
    }

    // == 주문 취소 ==//
    public void cancel(){
        this.product.addStock(quantity);
    }
}

