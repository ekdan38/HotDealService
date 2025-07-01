package com.hong.orderservice.domain;

import com.hong.orderservice.domain.base.TimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    @Column(nullable = true)
    private Long productId;

    // 응답 시에 productTitle 이 필요하다. => 반정규화
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal price;

    public OrderProduct(Long productId, String title, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.title = title;
        this.quantity = quantity;
        this.price = price;
    }

    // == 생성 메서드 ==
    public static OrderProduct create(Long productId, String title, Integer quantity, BigDecimal price){
        return new OrderProduct(productId, title, quantity, price);
    }

    // == Order 에서 사용할 연관 관계 관련 메서드 ==
    protected void setOrder(Order order){
        this.order = order;
    }

    // == OrderProduct 의 price 구하는 메서드 ==
    public BigDecimal extractTotalPrice(){
        return this.price.multiply(BigDecimal.valueOf(this.quantity));
    }

}

