package com.hong.hotdeal.domain;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WishlistProduct> wishlistProducts = new ArrayList<>();

    private Wishlist(User user) {
        this.user = user;
    }

    // == 생성 메서드 ==
    public static Wishlist create(User user){
        return new Wishlist(user);
    }

    // == 연관 관계 메서드 ==
    public void addProduct(WishlistProduct wishlistProduct){
        this.wishlistProducts.add(wishlistProduct);
        wishlistProduct.setWishlist(this);
    }
}
