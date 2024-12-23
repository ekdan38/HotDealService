package com.hong.hotdeal.domain;

import com.hong.hotdeal.domain.base.TimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Product extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer stock = 0;

    private Product(String title, Integer price, Integer stock) {
        this.title = title;
        this.price = price;
        this.stock = stock;
    }

    // == 생성 메서드 ==
    public static Product create (String title, Integer price, Integer stock){
        return new Product(title, price, stock);
    }

    public void update(String title, Integer price, Integer stock){
        this.title = title;
        this.price = price;
        this.stock = stock;
    }

}
