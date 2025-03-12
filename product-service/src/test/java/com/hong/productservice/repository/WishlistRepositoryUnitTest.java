package com.hong.productservice.repository;

import com.hong.productservice.config.JpaConfig;
import com.hong.productservice.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class WishlistRepositoryUnitTest {

    @Autowired
    WishlistRepository wishlistRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ProductRepository productRepository;

    private Category category;
    @BeforeEach
    public void setUp() {
        category = Category.create("category");
        categoryRepository.save(category);
    }

    @Test
    @DisplayName("userId로 Wishlist 조회")
    public void findByUserId(){
        //given
        Long userId = 1L;

        Product product = Product.create("product", 100, 100, List.of(CategoryProduct.create(category)));
        productRepository.save(product);

        Wishlist wishlist = Wishlist.create(userId);
        wishlist.addWishlistProducts(WishlistProduct.create(product, 10));
        wishlistRepository.save(wishlist);

        //when
        Optional<Wishlist> foundOptionalWishlist = wishlistRepository.findByUserId(userId);

        //then
        assertThat(foundOptionalWishlist).isPresent();
        Wishlist foundWishlist = foundOptionalWishlist.get();
        assertThat(foundWishlist.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("userId로 Wishlist 조회 및 WishlistProducts FETCH JOIN")
    public void findByUserIdWithProducts(){
        //given
        Long userId = 1L;

        Product product1 = Product.create("product1", 100, 100, List.of(CategoryProduct.create(category)));
        Product product2 = Product.create("product2", 100, 100, List.of(CategoryProduct.create(category)));
        productRepository.save(product1);
        productRepository.save(product2);

        Wishlist wishlist = Wishlist.create(userId);
        wishlist.addWishlistProducts(WishlistProduct.create(product1, 5));
        wishlist.addWishlistProducts(WishlistProduct.create(product2, 5));
        wishlistRepository.save(wishlist);

        //when
        Optional<Wishlist> foundOptionalWishlist = wishlistRepository.findByUserIdWithProducts(userId);

        //then
        assertThat(foundOptionalWishlist).isPresent();
        Wishlist foundWishlist = foundOptionalWishlist.get();
        assertThat(foundWishlist.getUserId()).isEqualTo(userId);
        assertThat(foundWishlist.getWishlistProducts().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("userId로 Wishlist 조회 및 WishlistProducts, Product FETCH JOIN")
    public void findWithProductsByUserId(){
        //given
        Long userId = 1L;

        Product product1 = Product.create("product1", 100, 100, List.of(CategoryProduct.create(category)));
        Product product2 = Product.create("product2", 100, 100, List.of(CategoryProduct.create(category)));
        productRepository.save(product1);
        productRepository.save(product2);

        Wishlist wishlist = Wishlist.create(userId);
        wishlist.addWishlistProducts(WishlistProduct.create(product1, 5));
        wishlist.addWishlistProducts(WishlistProduct.create(product2, 5));
        wishlistRepository.save(wishlist);

        Long productId = product1.getId();
        //when
        Optional<Wishlist> foundOptionalWishlist = wishlistRepository.findByUserIdWithProducts(userId);

        //then
        assertThat(foundOptionalWishlist).isPresent();
        Wishlist foundWishlist = foundOptionalWishlist.get();
        assertThat(foundWishlist.getUserId()).isEqualTo(userId);
        assertThat(foundWishlist.getWishlistProducts().size()).isEqualTo(2);
        assertThat(foundWishlist.getWishlistProducts().get(0).getProduct().getId()).isEqualTo(productId);
    }
}