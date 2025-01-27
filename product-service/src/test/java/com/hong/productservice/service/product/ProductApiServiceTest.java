package com.hong.productservice.service.product;

import com.hong.common.dto.ProductCommonDto;
import com.hong.productservice.domain.Category;
import com.hong.productservice.domain.CategoryProduct;
import com.hong.productservice.domain.Product;
import com.hong.productservice.repository.CategoryRepository;
import com.hong.productservice.repository.ProductRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ProductApiServiceTest {

    @Autowired
    ProductApiService productApiService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    List<Product> products = new ArrayList<>();

    @BeforeEach
    void setup(){
        Category category = Category.create("category");
        categoryRepository.save(category);

        Product product1 = Product.create("product1", 10000, 100, List.of(CategoryProduct.create(category)));
        productRepository.save(product1);
        products.add(product1);

        Product product2 = Product.create("product2", 20000, 100,  List.of(CategoryProduct.create(category)));
        productRepository.save(product2);
        products.add(product2);
    }
    @AfterEach
    void after(){
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("멀티 스레드 Product 조회, 재고 감소_성공")
    public void fetchAndDecreaseStock_success() throws InterruptedException {
        //given
        List<ProductCommonDto> productCommonDtos = new ArrayList<>();
        for (Product product : products) {
            productCommonDtos.add(new ProductCommonDto(product.getId(), 1));
        }

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        //when
        for(int i = 0; i < numberOfThreads; i++){
            executorService.submit(() -> {
               try{
                   productApiService.decreaseStock(productCommonDtos);
               }
               finally {
                   latch.countDown();
               }
            });
        }
        latch.await();

        //then
        for (Product product : products) {
            Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(foundProduct.getStock()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("멀티 스레드 Product 조회, 재고 증가_성공")
    public void fetchAndIncreaseStock_success() throws InterruptedException {
        //given
        List<ProductCommonDto> productCommonDtos = new ArrayList<>();
        for (Product product : products) {
            productCommonDtos.add(new ProductCommonDto(product.getId(), 1));
        }

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        //when
        for(int i = 0; i < numberOfThreads; i++){
            executorService.submit(() -> {
                try{
                    productApiService.increaseStock(productCommonDtos);
                }
                finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        //then
        for (Product product : products) {
            Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(foundProduct.getStock()).isEqualTo(200);
        }
    }

}