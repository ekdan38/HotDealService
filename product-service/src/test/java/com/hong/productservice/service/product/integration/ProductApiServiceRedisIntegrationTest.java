package com.hong.productservice.service.product.integration;

import com.hong.productservice.domain.Category;
import com.hong.productservice.domain.CategoryProduct;
import com.hong.productservice.domain.Product;
import com.hong.productservice.dto.product.ProductCacheDto;
import com.hong.productservice.dto.product.ProductStockDto;
import com.hong.productservice.repository.CategoryRepository;
import com.hong.productservice.repository.ProductRepository;
import com.hong.productservice.service.product.ProductApiService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class ProductApiServiceRedisIntegrationTest {

    @Autowired
    ProductApiService productApiService;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void clearRedis(){
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @Transactional
    @DisplayName("상품 조회(재고 포함) 및 cacheMiss 발생 캐시 저장")
    public void getProductsWithStock_cacheMiss(){
        //given
        Category category = Category.create("category");
        categoryRepository.save(category);

        ArrayList<Long> productIds = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>();
        HashMap<Long, Product> productMap = new HashMap<>();
        for(int i = 1; i < 3; i++){
            Product product = Product.create("product" + i, 100, 100, List.of(CategoryProduct.create(category)));
            productRepository.save(product);
            productIds.add(product.getId());
            keys.add("getProduct::products:" + product.getId());
            productMap.put(product.getId(), product);
        }

        ArrayList<Long> request = new ArrayList<>();
        request.add(productIds.get(0));
        request.add(productIds.get(1));

        //when
        List<ProductStockDto> result = productApiService.getProductsWithStock(request);

        //then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductId()).isEqualTo(productIds.get(0));
        assertThat(result.get(1).getProductId()).isEqualTo(productIds.get(1));

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<Object> cachedResultObject = valueOperations.multiGet(keys);
        for (Object object : cachedResultObject) {
            ProductCacheDto cachedResult = (ProductCacheDto) object;
            Product product = productMap.get(cachedResult.getId());
            assertThat(cachedResult.getId()).isEqualTo(product.getId());
            assertThat(cachedResult.getTitle()).isEqualTo(product.getTitle());
            assertThat(cachedResult.getPrice()).isEqualTo(product.getPrice());
        }
    }

    @Test
    @Transactional
    @DisplayName("상품 조회(재고 포함) 및 cacheHit")
    public void getProductsWithStock_cacheHit(){
        //given
        Category category = Category.create("category");
        categoryRepository.save(category);

        ArrayList<Long> productIds = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>();
        HashMap<Long, Product> productMap = new HashMap<>();
        for(int i = 1; i < 3; i++){
            Product product = Product.create("product" + i, 100, 100, List.of(CategoryProduct.create(category)));
            productRepository.save(product);
            productIds.add(product.getId());
            keys.add("getProduct::products:" + product.getId());
            productMap.put(product.getId(), product);
        }

        ArrayList<Long> request = new ArrayList<>();
        request.add(productIds.get(0));
        request.add(productIds.get(1));
        productApiService.getProductsWithStock(request);

        //when
        List<ProductStockDto> result = productApiService.getProductsWithStock(request);

        //then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductId()).isEqualTo(productIds.get(0));
        assertThat(result.get(1).getProductId()).isEqualTo(productIds.get(1));
        for (ProductStockDto dto : result) {
            Product product = productMap.get(dto.getProductId());
            assertThat(dto.getProductId()).isEqualTo(product.getId());
            assertThat(dto.getTitle()).isEqualTo(product.getTitle());
            assertThat(dto.getPrice()).isEqualTo(product.getPrice());
        }

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<Object> cachedResultObject = valueOperations.multiGet(keys);

        for (Object object : cachedResultObject) {
            ProductCacheDto cachedResult = (ProductCacheDto) object;
            Product product = productMap.get(cachedResult.getId());
            assertThat(cachedResult.getId()).isEqualTo(product.getId());
            assertThat(cachedResult.getTitle()).isEqualTo(product.getTitle());
            assertThat(cachedResult.getPrice()).isEqualTo(product.getPrice());
        }
    }

}
