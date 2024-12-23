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
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    private String title;

    // Category 계층 구조 표현
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> childs = new ArrayList<>();

    private Category(String title) {
        this.title = title;
    }

    // == 생성 메서드 ==
    public static Category create(String title){
        return new Category(title);
    }

    // == 부모 카테고리에서 자식 카테고리 설정 ==
    public void addChildrenCategory(Category child){
        this.childs.add(child);
        setParent(this);
    }

    // == 자식 카테고리에서 부모 카테고리 설정 ==
    public void setParent(Category parent){
        this.parent = parent;
        parent.getChilds().add(this);
    }
}
