package com.hong.productservice.service.product;

import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.productservice.domain.Category;
import com.hong.productservice.domain.CategoryProduct;
import com.hong.productservice.domain.Product;
import com.hong.productservice.repository.CategoryRepository;
import com.hong.productservice.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ProductApiServiceConcurrencyIntegrationTest {

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

        for(int i = 1; i <= 2; i++){
            Product product = Product.create("product" + i, 10000, 100, List.of(CategoryProduct.create(category)));
            productRepository.save(product);
            products.add(product);
        }
    }
    @AfterEach
    void after(){
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("멀티 스레드 Product 조회, 재고 감소_성공")
    public void fetchAndDecreaseStock_success() throws InterruptedException {
        //given
        List<ProductStockUpdateRequestDto> requestDtos = new ArrayList<>();
        for (Product product : products) {
            requestDtos.add(new ProductStockUpdateRequestDto(product.getId(), 1));
        }

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        //when
        for(int i = 0; i < numberOfThreads; i++){
            executorService.submit(() -> {
               try{
                   productApiService.decreaseStock(requestDtos);
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
        List<ProductStockUpdateRequestDto> productStockUpdateRequestDtos = new ArrayList<>();
        for (Product product : products) {
            productStockUpdateRequestDtos.add(new ProductStockUpdateRequestDto(product.getId(), 1));
        }

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        //when
        for(int i = 0; i < numberOfThreads; i++){
            executorService.submit(() -> {
                try{
                    productApiService.increaseStock(productStockUpdateRequestDtos);
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