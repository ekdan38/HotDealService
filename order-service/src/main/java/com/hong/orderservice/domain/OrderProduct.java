package com.hong.orderservice.domain;

import com.hong.common.entity.TimeEntity;
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

    @Column(nullable = false)
    private Long productId;
    @Column(nullable = true)
    private Long hotDealProductId;

    // 응답 시에 productTitle 이 필요하다. => 반정규화
    @Column(nullable = false)
    private String productTitle;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer price;

    private OrderProduct(Long productId, Long hotDealProductId, String productTitle, Integer quantity, Integer price) {
        this.productId = productId;
        this.hotDealProductId = hotDealProductId;
        this.productTitle = productTitle;
        this.quantity = quantity;
        this.price = price;
    }

    // == 생성 메서드 ==
    public static OrderProduct create(Long productId, String productTitle, Integer quantity, Integer price){
        return new OrderProduct(productId, null, productTitle, quantity, price);
    }
    public static OrderProduct create(Long productId, Long hotDealProductId, String productTitle, Integer quantity, Integer price){
        return new OrderProduct(productId, hotDealProductId, productTitle, quantity, price);
    }

    // == Order 에서 사용할 연관 관계 관련 메서드 ==
    protected void setOrder(Order order){
        this.order = order;
    }

    // == 해당 상품에 대한 총 가격 return 메서드 ==
    protected Integer getTotalPrice(){
        return quantity * price;
    }
}

