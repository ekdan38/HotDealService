package com.hong.hotdeal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class WishlistProduct {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wishlist_id", nullable = false)
    private Wishlist wishlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity = 0;

    private WishlistProduct(Product product, Integer quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    // == 생성 메서드 ==
    public static WishlistProduct create(Product product, Integer quantity){
        return new WishlistProduct(product, quantity);
    }

    // == 수량 변경 메서드 ==
    public void addQuantity(int quantity){
        this.quantity += quantity;
    }

    // == 수량 변경 메서드 ==
    public void updateQuantity(int quantity){
        this.quantity = quantity;
    }

    // == wishlist 에서 사용하는 연관관계 메서드 ==
    public void setWishlist(Wishlist wishlist){
        this.wishlist = wishlist;
    }
}
