package com.hong.productservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Wishlist {


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_id")
    private Long id;

    private Long userId;

    @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WishlistProduct> wishlistProducts = new ArrayList<>();

    private Wishlist(Long userId) {
        this.userId = userId;
    }

    // == 생성 메서드 ==
    public static Wishlist create(Long userId){
        return new Wishlist(userId);
    }

    // == 연관 관계 메서드 ==
    public void addWishlistProducts(WishlistProduct wishlistProduct){
        this.wishlistProducts.add(wishlistProduct);
        wishlistProduct.setWishlist(this);
    }
}
