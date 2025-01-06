package com.hong.productservice.domain;

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

    private CategoryProduct(Category category) {
        this.category = category;
    }

    // == 생성 메서드 ==
    public static CategoryProduct create(Category category){
        return new CategoryProduct(category);
    }

    // == Product 에서 사용할 연관관계 메서드 ==
    public void setProduct(Product product){
        this.product = product;
    }

}
