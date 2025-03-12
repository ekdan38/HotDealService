package com.hong.productservice.repository;

import com.hong.productservice.config.JpaConfig;
import com.hong.productservice.domain.Category;
import com.hong.productservice.domain.CategoryProduct;
import com.hong.productservice.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
class CategoryRepositoryUnitTest {

    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ProductRepository productRepository;

    @Test
    @DisplayName("root Category 존재 여부 확인")
    public void existsByParentIsNullAndTitle(){
        //given
        Category category = Category.create("rootCategory");
        categoryRepository.save(category);

        //when
        boolean exists = categoryRepository.existsByParentIsNullAndTitle(category.getTitle());

        //then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("parent Category 아래 childCategory 존재 여부 확인")
    public void existsByTitleAndParentId(){
        //given
        Category parentCategory = Category.create("parent");
        Category childCategory = Category.create("child");
        parentCategory.addChildrenCategory(childCategory);
        categoryRepository.save(childCategory);
        categoryRepository.save(parentCategory);

        //when
        boolean exists = categoryRepository.existsByTitleAndParentId("child", parentCategory.getId());

        //then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("categoryId 로 Category 조회 및 childrenCategory 도 함께 조회")
    public void findCategoryByCategoryIdWithChildren(){
        //given
        Category parentCategory = Category.create("parent");
        Category childCategory1 = Category.create("child1");
        Category childCategory2 = Category.create("child1");
        parentCategory.addChildrenCategory(childCategory1);
        parentCategory.addChildrenCategory(childCategory2);
        categoryRepository.save(childCategory1);
        categoryRepository.save(childCategory2);
        categoryRepository.save(parentCategory);

        //when
        List<Category> categories = categoryRepository.findCategoryByCategoryIdWithChildren(parentCategory.getId());

        //then
        assertThat(categories).isNotEmpty();
        Category fetchedParent = categories.stream()
                .filter(c -> c.getId().equals(parentCategory.getId()))
                .findFirst()
                .orElse(null);
        assertThat(fetchedParent).isNotNull();
        assertThat(fetchedParent.getChilds()).hasSize(2);
    }

    @Test
    @DisplayName("categoryId 로 Category 조회 및 LEFT JOIN 으로 CategoryProduct 조회")
    public void existsProductCategoryByCategoryId(){
        //given
        Category category = Category.create("category");
        categoryRepository.save(category);

        Product product = Product.create("product", 10000, 100, List.of(CategoryProduct.create(category)));
        productRepository.save(product);

        //when
        boolean exists = categoryRepository.existsProductCategoryByCategoryId(category.getId());

        //then
        assertThat(exists).isTrue();
    }
}