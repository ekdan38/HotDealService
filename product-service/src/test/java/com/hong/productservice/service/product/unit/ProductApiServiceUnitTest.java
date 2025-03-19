package com.hong.productservice.service.product.unit;

import com.hong.common.dto.ProductStockCheckRequestDto;
import com.hong.common.dto.ProductStockCheckResponseDto;
import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.common.exception.custom.ProductException;
import com.hong.productservice.domain.Product;
import com.hong.productservice.dto.product.ProductCacheDto;
import com.hong.productservice.dto.product.ProductStockDto;
import com.hong.productservice.dto.product.ProductStockProjection;
import com.hong.productservice.repository.ProductRepository;
import com.hong.productservice.service.product.ProductApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductApiServiceUnitTest {

    @InjectMocks
    ProductApiService productApiService;
    @Mock
    ProductRepository productRepository;
    @Mock
    RedissonClient redissonClient;
    @Mock
    RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private Product createTestProduct(long id, String title, int price, int stock) {
        Product product = Product.create(title, price, stock, List.of());
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    @Test
    @DisplayName("product 단건 조회_product 반환_성공")
    public void getProduct_success(){
        //given
        Long productId = 1L;
        String productTitle = "product";
        int price = 1000;
        int stock = 100;
        Product product = Product.create(productTitle, price, stock, List.of());
        ReflectionTestUtils.setField(product, "id", productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        //when
        Product result = productApiService.getProduct(productId);

        //then
        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getTitle()).isEqualTo(productTitle);
        assertThat(result.getPrice()).isEqualTo(price);
        assertThat(result.getStock()).isEqualTo(stock);
    }

    @Test
    @DisplayName("product 단건 조회_product 반환_실패_존재 하지 않는 product")
    public void getProduct_failure_notFoundProduct(){
        //given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        //when && then
        assertThatThrownBy(() -> productApiService.getProduct(productId)).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("products 조회(stock 포함)_성공_부분 cacheHit")
    public void getProductsWithStock_success_part_cacheHit(){
        //given
        ArrayList<Object> cacheHitProductStockDtos = new ArrayList<>();
        ArrayList<Long> cacheHitProductIds = new ArrayList<>();
        ArrayList<Product> cacheMissedProducts = new ArrayList<>();
        List<ProductStockProjection> stockProjections = new ArrayList<>();
        ArrayList<Long> productIds = new ArrayList<>();
        for(long i = 1; i <= 10; i++){
            productIds.add(i);
            Product product = createTestProduct(i, "product" + i, 1000, 100);
            ReflectionTestUtils.setField(product, "id", i);
            // cacheHit
            if(i % 2 == 0) {
                cacheHitProductStockDtos.add(new ProductCacheDto(i, "product" + i, 1000, List.of()));
                stockProjections.add(new ProductStockProjection(product.getId(), product.getStock()));
                cacheHitProductIds.add(product.getId());
            }
            // cacheMiss
            else {
                cacheHitProductStockDtos.add(null);
                cacheMissedProducts.add(product);
            }
        }

        // Redis 조회 key
        List<String> keys = productIds.stream().map(id -> "getProduct::products:" + id).toList();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().multiGet(keys)).thenReturn(cacheHitProductStockDtos);

        // cacheMiss Products 조회
        List<Long> cacheMissProductIds = cacheMissedProducts.stream().map(p -> p.getId()).toList();
        when(productRepository.findByIdsWithCategory(cacheMissProductIds)).thenReturn(cacheMissedProducts);

        // cacheHit stock 조회
        when(productRepository.findStockByProductIds(cacheHitProductIds)).thenReturn(stockProjections);

        //when
        List<ProductStockDto> result = productApiService.getProductsWithStock(productIds);

        //then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(10);
        result.forEach(r -> {
            assertThat(r.getStock()).isEqualTo(100);
            assertThat(r.getTitle()).isEqualTo("product" + r.getProductId());
            assertThat(r.getPrice()).isEqualTo(1000);
        });
    }

    @Test
    @DisplayName("products 조회(stock 포함)_실패_부분_cacheMiss_product 조회_존재 하지 않는 product")
    public void getProductsWithStock_failure_fetchCacheMiss_notFoundProduct(){
        //given
        ArrayList<Object> cacheHitProductStockDtos = new ArrayList<>();
        ArrayList<Long> cacheHitProductIds = new ArrayList<>();
        ArrayList<Product> cacheMissedProducts = new ArrayList<>();
        List<ProductStockProjection> stockProjections = new ArrayList<>();
        ArrayList<Long> productIds = new ArrayList<>();
        for(long i = 1; i <= 10; i++){
            productIds.add(i);
            Product product = createTestProduct(i, "product" + i, 1000, 100);
            ReflectionTestUtils.setField(product, "id", i);
            // cacheHit
            if(i % 2 == 0) {
                cacheHitProductStockDtos.add(new ProductCacheDto(i, "product" + i, 1000, List.of()));
                stockProjections.add(new ProductStockProjection(product.getId(), product.getStock()));
                cacheHitProductIds.add(product.getId());
            }
            // cacheMiss
            else {
                cacheHitProductStockDtos.add(null);
                cacheMissedProducts.add(product);
            }
        }

        // Redis 조회 key
        List<String> keys = productIds.stream().map(id -> "getProduct::products:" + id).toList();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().multiGet(keys)).thenReturn(cacheHitProductStockDtos);

        // cacheMiss Products 조회
        List<Long> cacheMissProductIds = cacheMissedProducts.stream().map(p -> p.getId()).toList();
        when(productRepository.findByIdsWithCategory(cacheMissProductIds)).thenReturn(List.of());

        // when && then
        assertThatThrownBy(() -> productApiService.getProductsWithStock(productIds)).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("products 조회(stock 포함)_실패_부분_cacheMiss_stock 조회_존재 하지 않는 product")
    public void getProductsWithStock_failure_fetchStock_notFoundProduct(){
        //given
        ArrayList<Object> cacheHitProductStockDtos = new ArrayList<>();
        ArrayList<Long> cacheHitProductIds = new ArrayList<>();
        ArrayList<Product> cacheMissedProducts = new ArrayList<>();
        List<ProductStockProjection> stockProjections = new ArrayList<>();
        ArrayList<Long> productIds = new ArrayList<>();
        for(long i = 1; i <= 10; i++){
            productIds.add(i);
            Product product = createTestProduct(i, "product" + i, 1000, 100);
            ReflectionTestUtils.setField(product, "id", i);
            // cacheHit
            if(i % 2 == 0) {
                cacheHitProductStockDtos.add(new ProductCacheDto(i, "product" + i, 1000, List.of()));
                stockProjections.add(new ProductStockProjection(product.getId(), product.getStock()));
                cacheHitProductIds.add(product.getId());
            }
            // cacheMiss
            else {
                cacheHitProductStockDtos.add(null);
                cacheMissedProducts.add(product);
            }
        }

        // Redis 조회 key
        List<String> keys = productIds.stream().map(id -> "getProduct::products:" + id).toList();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().multiGet(keys)).thenReturn(cacheHitProductStockDtos);

        // cacheMiss Products 조회
        List<Long> cacheMissProductIds = cacheMissedProducts.stream().map(p -> p.getId()).toList();
        when(productRepository.findByIdsWithCategory(cacheMissProductIds)).thenReturn(cacheMissedProducts);

        // cacheHit stock 조회
        when(productRepository.findStockByProductIds(cacheHitProductIds)).thenReturn(List.of());

        // when && then
        assertThatThrownBy(() -> productApiService.getProductsWithStock(productIds)).isInstanceOf(ProductException.class);

    }

    @Test
    @DisplayName("products 조회(재고 포함) 및 (요청 수량 < 재고)검증_성공")
    public void fetchProductAndValidateStock_success(){
        //given
        ArrayList<Object> cacheHitProductStockDtos = new ArrayList<>();
        ArrayList<Long> cacheHitProductIds = new ArrayList<>();
        ArrayList<Product> cacheMissedProducts = new ArrayList<>();
        List<ProductStockProjection> stockProjections = new ArrayList<>();
        ArrayList<Long> productIds = new ArrayList<>();
        for(long i = 1; i <= 2; i++){
            productIds.add(i);
            Product product = createTestProduct(i, "product" + i, 1000, 100);
            ReflectionTestUtils.setField(product, "id", i);
            // cacheHit
            if(i % 2 == 0) {
                cacheHitProductStockDtos.add(new ProductCacheDto(i, "product" + i, 1000, List.of()));
                stockProjections.add(new ProductStockProjection(product.getId(), product.getStock()));
                cacheHitProductIds.add(product.getId());
            }
            // cacheMiss
            else {
                cacheHitProductStockDtos.add(null);
                cacheMissedProducts.add(product);
            }
        }

        // request
        ArrayList<ProductStockCheckRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockCheckRequestDto(1L, 3));
        requestDtos.add(new ProductStockCheckRequestDto(2L, 3));

        // Redis 조회
        List<String> keys = productIds.stream().map(id -> "getProduct::products:" + id).toList();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().multiGet(keys)).thenReturn(cacheHitProductStockDtos);

        // cacheMiss Products 조회
        List<Long> cacheMissProductIds = cacheMissedProducts.stream().map(p -> p.getId()).toList();
        when(productRepository.findByIdsWithCategory(cacheMissProductIds)).thenReturn(cacheMissedProducts);

        // cacheHit stock 조회
        when(productRepository.findStockByProductIds(cacheHitProductIds)).thenReturn(stockProjections);

        //when
        List<ProductStockCheckResponseDto> result = productApiService.fetchProductAndValidateStock(requestDtos);

        //then
        result.forEach(r -> {
            assertThat(r.getTitle()).isEqualTo("product" + r.getProductId());
            assertThat(r.getRequestedQuantity()).isEqualTo(3);
            assertThat(r.getPrice()).isEqualTo(1000);
        });
    }

    @Test
    @DisplayName("products 조회(재고 포함) 및 (요청 수량 < 재고)검증_존재 하지 않는 product")
    public void fetchProductAndValidateStock_failure_notFoundProduct(){
        //given
        ArrayList<Object> cacheHitProductStockDtos = new ArrayList<>();
        ArrayList<Long> cacheHitProductIds = new ArrayList<>();
        ArrayList<Product> cacheMissedProducts = new ArrayList<>();
        List<ProductStockProjection> stockProjections = new ArrayList<>();
        ArrayList<Long> productIds = new ArrayList<>();
        for(long i = 1; i <= 2; i++){
            productIds.add(i);
            Product product = createTestProduct(i, "product" + i, 1000, 100);
            ReflectionTestUtils.setField(product, "id", i);
            // cacheHit
            if(i % 2 == 0) {
                cacheHitProductStockDtos.add(new ProductCacheDto(i, "product" + i, 1000, List.of()));
                stockProjections.add(new ProductStockProjection(product.getId(), product.getStock()));
                cacheHitProductIds.add(product.getId());
            }
            // cacheMiss
            else {
                cacheHitProductStockDtos.add(null);
                cacheMissedProducts.add(product);
            }
        }

        // request
        ArrayList<ProductStockCheckRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockCheckRequestDto(1L, 3));
        requestDtos.add(new ProductStockCheckRequestDto(2L, 3));

        // Redis 조회
        List<String> keys = productIds.stream().map(id -> "getProduct::products:" + id).toList();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().multiGet(keys)).thenReturn(cacheHitProductStockDtos);

        // cacheMiss Products 조회
        List<Long> cacheMissProductIds = cacheMissedProducts.stream().map(p -> p.getId()).toList();
        when(productRepository.findByIdsWithCategory(cacheMissProductIds)).thenReturn(cacheMissedProducts);

        // cacheHit stock 조회
        when(productRepository.findStockByProductIds(cacheHitProductIds)).thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> productApiService.fetchProductAndValidateStock(requestDtos)).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("products 조회(재고 포함) 및 (요청 수량 < 재고)검증_실패_재고 부족")
    public void fetchProductAndValidateStock_failure_notEnoughStock() {
        //given
        ArrayList<Object> cacheHitProductStockDtos = new ArrayList<>();
        ArrayList<Long> cacheHitProductIds = new ArrayList<>();
        ArrayList<Product> cacheMissedProducts = new ArrayList<>();
        List<ProductStockProjection> stockProjections = new ArrayList<>();
        ArrayList<Long> productIds = new ArrayList<>();
        for(long i = 1; i <= 2; i++){
            productIds.add(i);
            Product product = createTestProduct(i, "product" + i, 1000, 1);
            ReflectionTestUtils.setField(product, "id", i);
            // cacheHit
            if(i % 2 == 0) {
                cacheHitProductStockDtos.add(new ProductCacheDto(i, "product" + i, 1000, List.of()));
                stockProjections.add(new ProductStockProjection(product.getId(), product.getStock()));
                cacheHitProductIds.add(product.getId());
            }
            // cacheMiss
            else {
                cacheHitProductStockDtos.add(null);
                cacheMissedProducts.add(product);
            }
        }

        // request
        ArrayList<ProductStockCheckRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockCheckRequestDto(1L, 3));
        requestDtos.add(new ProductStockCheckRequestDto(2L, 3));

        // Redis 조회
        List<String> keys = productIds.stream().map(id -> "getProduct::products:" + id).toList();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().multiGet(keys)).thenReturn(cacheHitProductStockDtos);

        // cacheMiss Products 조회
        List<Long> cacheMissProductIds = cacheMissedProducts.stream().map(p -> p.getId()).toList();
        when(productRepository.findByIdsWithCategory(cacheMissProductIds)).thenReturn(cacheMissedProducts);

        // cacheHit stock 조회
        when(productRepository.findStockByProductIds(cacheHitProductIds)).thenReturn(stockProjections);

        //when && then
        assertThatThrownBy(() -> productApiService.fetchProductAndValidateStock(requestDtos)).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("products 재고 감소_성공")
    public void decreaseStock_success() throws InterruptedException {
        //given
        TransactionSynchronizationManager.initSynchronization();
        ArrayList<Long> productIds = new ArrayList<>();
        ArrayList<Product> foundProducts = new ArrayList<>();
        for(long i = 1; i <= 2; i++){
            Product product = createTestProduct(i, "product" + i, 1000, 100);
            ReflectionTestUtils.setField(product, "id", i);
            foundProducts.add(product);
            productIds.add(product.getId());
        }

        ArrayList<ProductStockUpdateRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockUpdateRequestDto(1L, 5));
        requestDtos.add(new ProductStockUpdateRequestDto(2L, 5));

        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(productRepository.findByIds(productIds)).thenReturn(foundProducts);

        //when
        List<ProductStockUpdateResponseDto> result = productApiService.decreaseStock(requestDtos);

        //then
        result.forEach(r -> {
            assertThat(r.getTitle()).isEqualTo("product" + r.getProductId());
            assertThat(r.getPrice()).isEqualTo(1000);
            assertThat(r.getRequestedQuantity()).isEqualTo(5);
        });
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("products 재고 감소_실패_존재 하지 않는 상품")
    public void decreaseStock_failure_notFoundProduct() throws InterruptedException {
        //given
        TransactionSynchronizationManager.initSynchronization();
        ArrayList<Long> productIds = new ArrayList<>();
        for(long i = 1; i <= 2; i++){
            Product product = createTestProduct(i, "product" + i, 1000, 100);
            ReflectionTestUtils.setField(product, "id", i);
            productIds.add(product.getId());
        }

        ArrayList<ProductStockUpdateRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockUpdateRequestDto(1L, 5));
        requestDtos.add(new ProductStockUpdateRequestDto(2L, 5));

        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(productRepository.findByIds(productIds)).thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> productApiService.decreaseStock(requestDtos)).isInstanceOf(ProductException.class);
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("products 재고 증가_성공")
    public void increaseStock_success() throws InterruptedException {
        //given
        TransactionSynchronizationManager.initSynchronization();
        ArrayList<Long> productIds = new ArrayList<>();
        ArrayList<Product> foundProducts = new ArrayList<>();
        for(long i = 1; i <= 2; i++){
            Product product = createTestProduct(i, "product" + i, 1000, 100);
            ReflectionTestUtils.setField(product, "id", i);
            foundProducts.add(product);
            productIds.add(product.getId());
        }

        ArrayList<ProductStockUpdateRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockUpdateRequestDto(1L, 5));
        requestDtos.add(new ProductStockUpdateRequestDto(2L, 5));

        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(productRepository.findByIds(productIds)).thenReturn(foundProducts);

        //when
        List<ProductStockUpdateResponseDto> result = productApiService.increaseStock(requestDtos);

        //then
        result.forEach(r -> {
            assertThat(r.getTitle()).isEqualTo("product" + r.getProductId());
            assertThat(r.getPrice()).isEqualTo(1000);
            assertThat(r.getRequestedQuantity()).isEqualTo(5);
        });
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("products 재고 감소_실패_존재 하지 않는 상품")
    public void increaseStock_failure_notFoundProduct() throws InterruptedException {
        //given
        TransactionSynchronizationManager.initSynchronization();
        ArrayList<Long> productIds = new ArrayList<>();
        for(long i = 1; i <= 2; i++){
            Product product = createTestProduct(i, "product" + i, 1000, 100);
            ReflectionTestUtils.setField(product, "id", i);
            productIds.add(product.getId());
        }

        ArrayList<ProductStockUpdateRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockUpdateRequestDto(1L, 5));
        requestDtos.add(new ProductStockUpdateRequestDto(2L, 5));

        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(productRepository.findByIds(productIds)).thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> productApiService.increaseStock(requestDtos)).isInstanceOf(ProductException.class);
        TransactionSynchronizationManager.clearSynchronization();
    }

}