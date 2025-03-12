package com.hong.productservice.repository;

import com.hong.productservice.config.JpaConfig;
import com.hong.productservice.domain.Category;
import com.hong.productservice.domain.CategoryProduct;
import com.hong.productservice.domain.Product;
import com.hong.productservice.dto.product.ProductResponseDto;
import com.hong.productservice.dto.product.ProductStockProjection;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
class ProductRepositoryUnitTest {

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

    private Long generateProducts() {
        Long maxId = 0L;
        for(long i = 1; i <= 10; i++){
            Product product;
            if(i % 2 == 0) product = Product.create("EvenProduct" + i, 100, 10, List.of(CategoryProduct.create(category)));
            else product = Product.create("OddProduct" + i, 100, 10, List.of(CategoryProduct.create(category)));
            productRepository.save(product);
            if(i == 10) maxId = product.getId();
        }
        return maxId;
    }

    @Test
    @DisplayName("동일한 title 의 Product 존재 확인")
    public void existsByTitle(){
        //given
        String productTitle = "product";
        Product product = Product.create(productTitle, 100, 100, List.of(CategoryProduct.create(category)));
        productRepository.save(product);

        //when
        boolean exists = productRepository.existsByTitle(productTitle);

        //then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("products 페이징 조회 CategoryId, Search 포함 조회")
    void testFindProductsByCursorAndCategoryIdAndSearchAndSize_withFilters() {
        // given
        Long maxId = generateProducts();
        Long cursor = maxId + 1L;
        int size = 5;
        Long categoryId = category.getId();
        String search = "Even";
        PageRequest pageRequest = PageRequest.of(0, size);

        // when
        List<ProductResponseDto> result = productRepository.findProductsByCursorAndCategoryIdAndSearchAndSize(cursor, categoryId, search, pageRequest);

        // then
        assertThat(result).hasSize(size);
        // 정렬 확인
        for(int i = 1; i < size; i++){
            assertThat(result.get(i - 1).getId()).isGreaterThan(result.get(i).getId());
        }
        // Search 조건 확인
        for (ProductResponseDto responseDto : result) {
            assertThat(responseDto.getTitle().contains("EvenProduct")).isTrue();
        }
    }

    @Test
    @DisplayName("products 페이징 조회 CategoryId, Search 미 포함 조회")
    void testFindProductsByCursorAndCategoryIdAndSearchAndSize_withNullFilters() {
        // given
        Long maxId = generateProducts();
        Long cursor = maxId + 1L;
        int size = 10;
        PageRequest pageRequest = PageRequest.of(0, size);

        // when
        List<ProductResponseDto> result = productRepository.findProductsByCursorAndCategoryIdAndSearchAndSize(cursor, null, null, pageRequest);

        // then
        assertThat(result).hasSize(size);
        // 정렬 확인
        for(int i = 1; i < size; i++){
            assertThat(result.get(i - 1).getId()).isGreaterThan(result.get(i).getId());
        }
    }

    @Test
    @DisplayName("productId 로 Product 조회 및 CategoryProduct FETCH JOIN")
    void findProductByProductIdWithCategoryProducts() {
        // given
        Product product = Product.create("product", 100, 100, List.of(CategoryProduct.create(category)));
        productRepository.save(product);

        // when
        Optional<Product> foundOptionalProduct = productRepository.findProductByProductIdWithCategoryProducts(product.getId());

        // then
        assertThat(foundOptionalProduct).isPresent();
        Product foundProduct = foundOptionalProduct.get();
        assertThat(foundProduct.getCategoryProducts().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("productIds 로 Products 조회")
    void findByIds() {
        // given
        ArrayList<Long> productIds = new ArrayList<>();
        Product product1 = Product.create("product1", 100, 100, List.of(CategoryProduct.create(category)));
        Product product2 = Product.create("product2", 100, 100, List.of(CategoryProduct.create(category)));
        productRepository.save(product1);
        productRepository.save(product2);
        productIds.add(product1.getId());
        productIds.add(product2.getId());

        // when
        List<Product> foundProducts = productRepository.findByIds(productIds);

        // then
        assertThat(foundProducts.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("productIds 로 Products 조회 및 Category FETCH JOIN")
    void findByIdsWithCategory() {
        // given
        ArrayList<Long> productIds = new ArrayList<>();
        Product product1 = Product.create("product1", 100, 100, List.of(CategoryProduct.create(category)));
        Product product2 = Product.create("product2", 100, 100, List.of(CategoryProduct.create(category)));
        productRepository.save(product1);
        productRepository.save(product2);
        productIds.add(product1.getId());
        productIds.add(product2.getId());

        // when
        List<Product> foundProducts = productRepository.findByIds(productIds);

        // then
        assertThat(foundProducts.size()).isEqualTo(2);
        assertThat(foundProducts.get(0).getCategoryProducts().size()).isEqualTo(1);
        assertThat(foundProducts.get(0).getCategoryProducts().get(0).getCategory().getTitle()).isEqualTo("category");
    }

    @Test
    @DisplayName("productIds 로 Id, Stock DtoProjection 조회")
    void findStockByProductIds() {
        // given
        ArrayList<Long> productIds = new ArrayList<>();
        Product product1 = Product.create("product1", 100, 100, List.of(CategoryProduct.create(category)));
        Product product2 = Product.create("product2", 100, 100, List.of(CategoryProduct.create(category)));
        productRepository.save(product1);
        productRepository.save(product2);
        productIds.add(product1.getId());
        productIds.add(product2.getId());

        // when
        List<ProductStockProjection> foundProductsStock = productRepository.findStockByProductIds(productIds);

        // then
        assertThat(foundProductsStock.size()).isEqualTo(2);
        assertThat(foundProductsStock.get(0).getGetId()).isEqualTo(productIds.get(0));
        assertThat(foundProductsStock.get(0).getGetStock()).isEqualTo(100);
    }





}