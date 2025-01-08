package com.hong.productservice.domain;

import com.hong.common.entity.TimeEntity;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.ProductException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryProduct> categoryProducts = new ArrayList<>();

    private Product(String title, Integer price, Integer stock) {
        this.title = title;
        this.price = price;
        this.stock = stock;
    }

    // == 생성 메서드 ==
    public static Product create (String title, Integer price, Integer stock, List<CategoryProduct> categoryProducts){
        Product product = new Product(title, price, stock);
        product.addCategoryProducts(categoryProducts);
        return product;
    }

    public void update(String title, Integer price, Integer stock){
        this.title = title;
        this.price = price;
        this.stock = stock;
    }

    // == CategoryProducts 연관관계 메서드 ==
    public void addCategoryProducts(List<CategoryProduct> categoryProducts) {
        categoryProducts.forEach(this::addCategoryProduct);
    }

    private void addCategoryProduct(CategoryProduct categoryProduct) {
        this.categoryProducts.add(categoryProduct);
        categoryProduct.setProduct(this);
    }

    // == CategoryProducts 연관관계 메서드 ==
    public void removeCategoryProducts(List<CategoryProduct> categoryProducts) {
        this.categoryProducts.removeAll(categoryProducts);
    }

    // == stock 감소 메서드 ==
    public void decreaseStock(Integer quantity){
        if(this.stock - quantity < 0){
            throw new ProductException(ErrorCode.ORDER_PRODUCT_NO_STOCK);
        }
        this.stock -= quantity;
    }

    // == stock 증가 메서드 ==
    public void increaseStock(Integer quantity){
        this.stock += quantity;
    }

}
