package com.hong.productservice.service.product.integration;

import com.hong.productservice.domain.Category;
import com.hong.productservice.domain.CategoryProduct;
import com.hong.productservice.domain.Product;
import com.hong.productservice.dto.category.CategoryDto;
import com.hong.productservice.dto.product.ProductCacheDto;
import com.hong.productservice.dto.product.ProductDto;
import com.hong.productservice.dto.product.ProductPagingResponseDto;
import com.hong.productservice.dto.product.ProductResponseDto;
import com.hong.productservice.repository.CategoryRepository;
import com.hong.productservice.repository.ProductRepository;
import com.hong.productservice.service.product.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class ProductServiceImplRedisIntegrationTest {

    @Autowired
    ProductServiceImpl productService;
    @MockitoSpyBean
    ProductRepository productRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void clearRedis(){
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    private Long categoryId = null;

    private Category createTestCategory(String title){
        Category category = Category.create("category");
        categoryRepository.save(category);
        categoryId = category.getId();
        return category;
    }

    private Product createTestProduct(String title, int price, int stock, Category category){
        Product product = Product.create(title, price, stock, List.of(CategoryProduct.create(category)));
        productRepository.save(product);
        return product;
    }

    private Long createTestProducts(String titlePrefix, int price, int stock, Category category){
        Long firstId = null;
        for(int i = 1; i <= 5; i++){
            Product product = createTestProduct(titlePrefix + i, price, stock, category);
            if(i == 1) firstId = product.getId();
        }
        return firstId;
    }

    @Test
    @Transactional
    @DisplayName("product 페이징 조회 및 cacheMiss 발생 캐시 저장")
    public void getProducts_cacheMiss(){
        // given
        Long cursor = null;
        String search = "product";
        int size = 5;

        String titlePrefix = "product";
        int price = 1000;
        int stock = 100;
        Category category = createTestCategory("category");
        Long expectedNextCursor = createTestProducts(titlePrefix, price, stock, category);

        //when
        ProductPagingResponseDto result = productService.getProducts(search, cursor, size, categoryId);

        //then
        assertThat(result.getNextCursor()).isEqualTo(expectedNextCursor);
        assertThat(result.getProducts()).hasSize(5);
        result.getProducts().forEach(pr -> {
            assertThat(pr.getTitle()).startsWith(titlePrefix);
            assertThat(pr.getPrice()).isEqualTo(price);
        });

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = "getProducts::products:cursor:" + cursor + ":size:" + size + ":categoryId:" + categoryId + ":search:" + search;
        ProductPagingResponseDto cachedResult = (ProductPagingResponseDto) valueOperations.get(key);

        assertThat(cachedResult).isNotNull();
        assertThat(cachedResult.getNextCursor()).isEqualTo(expectedNextCursor);
        assertThat(cachedResult.getProducts()).hasSize(5);
        cachedResult.getProducts().forEach(pr -> {
            assertThat(pr.getId()).isNotNull();
            assertThat(pr.getTitle()).startsWith(titlePrefix);
            assertThat(pr.getPrice()).isEqualTo(price);
        });
    }

    @Test
    @Transactional
    @DisplayName("product 페이징 조회 및 cacheHit")
    public void getProducts_cacheHit(){
        // given
        Long cursor = null;
        String search = "product";
        int size = 5;

        String titlePrefix = "product";
        int price = 1000;
        int stock = 100;
        Category category = createTestCategory("category");
        Long expectedNextCursor = createTestProducts(titlePrefix, price, stock, category);
        productService.getProducts(search, cursor, size, categoryId);

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = "getProducts::products:cursor:" + cursor + ":size:" + size + ":categoryId:" + categoryId + ":search:" + search;
        ProductPagingResponseDto cachedResult = (ProductPagingResponseDto) valueOperations.get(key);

        assertThat(cachedResult).isNotNull();
        assertThat(cachedResult.getNextCursor()).isEqualTo(expectedNextCursor);
        assertThat(cachedResult.getProducts()).hasSize(5);
        cachedResult.getProducts().forEach(pr -> {
            assertThat(pr.getId()).isNotNull();
            assertThat(pr.getTitle()).startsWith(titlePrefix);
            assertThat(pr.getPrice()).isEqualTo(price);
        });

        //when
        ProductPagingResponseDto result = productService.getProducts(search, cursor, size, categoryId);

        //then
        verify(productRepository, times(1))
                .findProductsByCursorAndCategoryIdAndSearchAndSize(anyLong(), anyLong(), anyString(), any());
        assertThat(result.getNextCursor()).isEqualTo(expectedNextCursor);
        assertThat(result.getProducts()).hasSize(5);
        result.getProducts().forEach(pr -> {
            assertThat(pr.getId()).isNotNull();
            assertThat(pr.getTitle()).startsWith(titlePrefix);
        });
    }

    @Test
    @Transactional
    @DisplayName("product 생성 및 getProducts 캐시 전면 무효화")
    public void createProduct_cacheEvict(){
        // given
        Long cursor = null;
        String search = "product";
        int size = 5;

        Category category = createTestCategory("category");
        String titlePrefix = "existsProduct";
        createTestProduct(titlePrefix, 1000, 100, category);
        productService.getProducts(search, cursor, size, categoryId);

        String newTitle = "newProduct";
        int stock = 100;
        int price = 100;
        ProductDto requestDto = new ProductDto(newTitle, stock, price, List.of(new CategoryDto(categoryId)));

        //when
        ProductResponseDto result = productService.createProduct(requestDto);

        //then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo(newTitle);
        assertThat(result.getPrice()).isEqualTo(price);

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = "getProducts::products:cursor:" + cursor + ":size:" + size + ":categoryId:" + categoryId + ":search:" + search;
        ProductPagingResponseDto cachedResult = (ProductPagingResponseDto) valueOperations.get(key);
        assertThat(cachedResult).isNull();
    }

    @Test
    @Transactional
    @DisplayName("product 단건 조회 및 cacheMiss 발생 캐시 저장")
    public void getProduct_cacheMiss(){
        //given
        String titlePrefix = "product";
        int price = 1000;
        int stock = 100;
        Category category = createTestCategory("category");
        Product product = createTestProduct(titlePrefix, price, stock, category);

        //when
        ProductCacheDto result = productService.getProduct(product.getId());

        //then
        assertThat(result.getId()).isEqualTo(product.getId());
        assertThat(result.getTitle()).isEqualTo(product.getTitle());
        assertThat(result.getPrice()).isEqualTo(product.getPrice());

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = "getProduct::products:" + product.getId();
        ProductCacheDto cachedResult = (ProductCacheDto)valueOperations.get(key);
        assertThat(cachedResult.getId()).isEqualTo(product.getId());
        assertThat(cachedResult.getTitle()).isEqualTo(product.getTitle());
        assertThat(cachedResult.getPrice()).isEqualTo(product.getPrice());
    }

    @Test
    @Transactional
    @DisplayName("product 단건 조회 및 cacheHit")
    public void getProduct_cacheHit(){
        //given
        String titlePrefix = "product";
        int price = 1000;
        int stock = 100;
        Category category = createTestCategory("category");
        Product product = createTestProduct(titlePrefix, price, stock, category);
        productService.getProduct(product.getId());

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = "getProduct::products:" + product.getId();
        ProductCacheDto cachedResult = (ProductCacheDto)valueOperations.get(key);
        assertThat(cachedResult.getId()).isEqualTo(product.getId());
        assertThat(cachedResult.getTitle()).isEqualTo(product.getTitle());
        assertThat(cachedResult.getPrice()).isEqualTo(product.getPrice());

        //when
        ProductCacheDto result = productService.getProduct(product.getId());

        //then
        verify(productRepository, times(1))
                .findProductByProductIdWithCategoryProducts(anyLong());
        assertThat(result.getId()).isEqualTo(product.getId());
        assertThat(result.getTitle()).isEqualTo(product.getTitle());
        assertThat(result.getPrice()).isEqualTo(product.getPrice());
    }

    @Test
    @Transactional
    @DisplayName("product 수정 및 페이징 조회 부분 무효화, 단건 조회 부분 무효화")
    public void updateProduct_cacheEvict(){
        //given
        String titlePrefix = "product";
        int price = 1000;
        int stock = 100;
        Category category = createTestCategory("category");
        Product product = createTestProduct(titlePrefix, price, stock, category);
        productService.getProduct(product.getId());

        Long cursor = null;
        String search = "product";
        int size = 10;
        createTestProducts(titlePrefix, price, stock, category);
        productService.getProducts(search, cursor, size, categoryId);

        String newTitle = "mewTitle";
        int newPrice = 10000;
        int newStock = 1000;
        ProductDto requestDto = new ProductDto(newTitle, newPrice, newStock, List.of(new CategoryDto(categoryId)));

        //when
        ProductResponseDto result = productService.updateProduct(product.getId(), requestDto);

        //then
        assertThat(result.getId()).isEqualTo(product.getId());
        assertThat(result.getTitle()).isEqualTo(newTitle);
        assertThat(result.getPrice()).isEqualTo(newPrice);
        assertThat(result.getStock()).isEqualTo(newStock);
        assertThat(result.getCategories().get(0).getTitle()).isEqualTo(category.getTitle());

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String getProductsKey = "getProducts::products:cursor:" + cursor + ":size:" + size + ":categoryId:" + categoryId + ":search:" + search;
        ProductPagingResponseDto getProductsCachedResult = (ProductPagingResponseDto) valueOperations.get(getProductsKey);
        assertThat(getProductsCachedResult).isNull();

        String getProductKey = "getProduct::products:" + product.getId();
        ProductCacheDto getProductCachedResult = (ProductCacheDto)valueOperations.get(getProductKey);
        assertThat(getProductCachedResult).isNull();
    }

    @Test
    @Transactional
    @DisplayName("product 삭제 및 페이징 조회 부분 무효화, 단건 조회 부분 무효화 ")
    public void deleteProduct_cacheEvict(){
        //given
        String titlePrefix = "product";
        int price = 1000;
        int stock = 100;
        Category category = createTestCategory("category");
        Product product = createTestProduct(titlePrefix, price, stock, category);
        productService.getProduct(product.getId());

        Long cursor = null;
        String search = "product";
        int size = 10;
        createTestProducts(titlePrefix, price, stock, category);
        productService.getProducts(search, cursor, size, categoryId);

        //when
        ProductResponseDto result = productService.deleteProduct(product.getId());

        //then
        assertThat(result.getId()).isEqualTo(product.getId());

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String getProductsKey = "getProducts::products:cursor:" + cursor + ":size:" + size + ":categoryId:" + categoryId + ":search:" + search;
        ProductPagingResponseDto getProductsCachedResult = (ProductPagingResponseDto) valueOperations.get(getProductsKey);
        assertThat(getProductsCachedResult).isNull();

        String getProductKey = "getProduct::products:" + product.getId();
        ProductCacheDto getProductCachedResult = (ProductCacheDto)valueOperations.get(getProductKey);
        assertThat(getProductCachedResult).isNull();
    }

}
