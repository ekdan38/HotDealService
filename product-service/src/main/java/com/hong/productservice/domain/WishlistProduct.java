package com.hong.productservice.domain;

import com.hong.productservice.domain.base.TimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class WishlistProduct extends TimeEntity {


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

    // == wishlist 에서 사용하는 연관관계 메서드 ==
    protected void setWishlist(Wishlist wishlist){
        this.wishlist = wishlist;
    }

    public void addQuantity(Integer quantity){
        this.quantity += quantity;
    }

    public void updateQuantity(Integer quantity){
        this.quantity = quantity;
    }
}
