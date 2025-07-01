package com.hong.userservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "cart_item",
        indexes = {
        @Index(name = "idx_user_product_full", columnList = "user_id, cart_id, product_id, quantity")
})
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    private CartItem(Long userId, Long productId, Integer quantity) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
    }

    // == 생성 메서드 ==
    public static CartItem create(Long userId, Long productId, Integer quantity) {
        return new CartItem(userId, productId, quantity);
    }

    // == quantity 변경 메서드 ==
    public void updateQuantity(int quantity){
        this.quantity = quantity;
    }
}
