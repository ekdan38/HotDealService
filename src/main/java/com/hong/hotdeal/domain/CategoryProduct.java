package com.hong.hotdeal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryProduct {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private CategoryProduct(Category category, Product product) {
        this.category = category;
        this.product = product;
    }

    // == 생성 메서드 ==
    public static CategoryProduct create(Category category, Product product){
        return new CategoryProduct(category, product);
    }

    // == 수정 메서드 ==
    public void update(Category category, Product product){
        this.category = category;
        this.product = product;
    }
}
