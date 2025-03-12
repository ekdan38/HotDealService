package com.hong.productservice.repository;

import com.hong.productservice.config.JpaConfig;
import com.hong.productservice.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class WishlistProductRepositoryUnitTest {

    @Autowired
    WishlistProductRepository wishlistProductRepository;
    @Autowired
    WishlistRepository wishlistRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CategoryRepository categoryRepository;

    private Category category;
    @BeforeEach
    public void setUp() {
        category = Category.create("category");
        categoryRepository.save(category);
    }

    @Test
    @DisplayName("wishlistProducts 페이징 조회")
    public void findByWishlistIdAndCursor(){
        //given
        Long userId = 1L;
        Wishlist wishlist = Wishlist.create(userId);
        for(int i = 1; i <= 4; i++){
            Product product;
            product = Product.create("product" + i, 100, 100, List.of(CategoryProduct.create(category)));
            productRepository.save(product);
            wishlist.addWishlistProducts(WishlistProduct.create(product, 5));
        }
        wishlistRepository.save(wishlist);

        Long cursor = Long.MAX_VALUE;
        int size = 3;
        PageRequest pageRequest = PageRequest.of(0, size);

        //when
        List<WishlistProduct> result = wishlistProductRepository.findByWishlistIdAndCursor(wishlist.getId(), cursor, pageRequest);

        //then
        assertThat(result).hasSize(size);
        // 정렬 확인
        for(int i = 1; i < size; i++){
            assertThat(result.get(i - 1).getId()).isGreaterThan(result.get(i).getId());
        }
    }

    @Test
    @DisplayName("wishlistId, productIds 로 wishlistProduct 조회 및 Product FETCH JOIN")
    public void findByWishlistIdAndProductIds(){
        //given
        Long userId = 1L;
        ArrayList<Long> productIds = new ArrayList<>();
        Wishlist wishlist = Wishlist.create(userId);
        for(int i = 1; i <= 4; i++){
            Product product;
            product = Product.create("product" + i, 100, 100, List.of(CategoryProduct.create(category)));
            productRepository.save(product);
            productIds.add(product.getId());
            wishlist.addWishlistProducts(WishlistProduct.create(product, 5));
        }
        wishlistRepository.save(wishlist);

        //when
        List<WishlistProduct> foundWishlistProduct = wishlistProductRepository.findByWishlistIdAndProductIds(wishlist.getId(), productIds);

        //then
        assertThat(foundWishlistProduct.size()).isEqualTo(4);
        assertThat(foundWishlistProduct.get(0).getProduct().getTitle()).isEqualTo("product1");
    }





}