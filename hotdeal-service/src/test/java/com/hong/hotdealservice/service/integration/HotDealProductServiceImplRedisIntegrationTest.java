package com.hong.hotdealservice.service.integration;

import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.HotDealProductCacheDto;
import com.hong.hotdealservice.dto.HotDealProductPagingResponseDto;
import com.hong.hotdealservice.dto.HotDealProductResponseDto;
import com.hong.hotdealservice.repository.HotDealProductRepository;
import com.hong.hotdealservice.repository.HotDealRepository;
import com.hong.hotdealservice.service.HotDealProductServiceImpl;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class HotDealProductServiceImplRedisIntegrationTest {

    @Autowired
    HotDealProductServiceImpl hotDealProductService;
    @MockitoSpyBean
    HotDealProductRepository hotDealProductRepository;
    @Autowired
    HotDealRepository hotDealRepository;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;
    @Autowired
    EntityManager em;

    private HotDealProduct createTestHotDealProduct(Long productId, String productTitle){
        return HotDealProduct.create(productId, productTitle, 1000, 0.1, 100);
    }

    private HotDeal createTestHotDeal(List<HotDealProduct> hp){
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime startTime = now.minusDays(1);
        LocalDateTime endTime = now.plusDays(1);
        HotDeal hotDeal = HotDeal.create(1L, "hotDeal", "description", startTime, endTime, hp);
        hotDealRepository.save(hotDeal);
        em.flush();
        em.clear();
        return hotDeal;
    }

    private List<HotDealProduct> createTestHotDealProducts(String productTitlePrefix){
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        for(long i = 1; i <= 5; i++) {
            hotDealProducts.add(createTestHotDealProduct(i, productTitlePrefix + i));
        }
        return hotDealProducts;
    }

    private String getHotDealProductKey(Long hotDealProductId){
        return "getHotDealProduct::hotdeal_products:" + hotDealProductId;
    }
    private String gethotDealProductPagingKey(Long hotDealId, Long cursor, int size, String search){
        return "getHotDealProducts::hotdeal:" + hotDealId +
                "hotdeal_products:cursor:" + (cursor == null ? "" : cursor)
                + ":size:" + size + ":search:" + (search == null ? "" : search);
    }

    @AfterEach
    void clearRedis(){
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @Transactional
    @DisplayName("hotDealProducts 페이지 조회 및 cacheMiss 발생 캐시 저장")
    public void getHotDealProducts_cacheMiss(){
        //given
        String hotDealProductTitlePrefix = "product";
        List<HotDealProduct> hotDealProducts = createTestHotDealProducts(hotDealProductTitlePrefix);
        HotDeal hotDeal = createTestHotDeal(hotDealProducts);
        int size = 3;
        int hotDealProductSize = hotDeal.getHotDealProducts().size();
        Long expectedCursor = hotDeal.getHotDealProducts().get(hotDealProductSize - size).getId();
        Long cursor = null;
        String search = null;

        //when
        HotDealProductPagingResponseDto result = hotDealProductService.getHotDealProducts(hotDeal.getId(), null, null, size);

        //then
        verify(hotDealProductRepository, times(1))
                .findByCursorAndSearchAndSizeHotDealProducts(eq(hotDeal.getId()), eq(Long.MAX_VALUE), eq(search), any(Pageable.class));

        assertThat(result.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(result.getCursor()).isEqualTo(expectedCursor);
        List<HotDealProductResponseDto> resultHotDealProducts = result.getHotDealProducts();
        assertThat(resultHotDealProducts).hasSize(size);

        ValueOperations<String, Object> objectValueOperations = redisTemplate.opsForValue();
        String key = gethotDealProductPagingKey(hotDeal.getId(), cursor, size, search);
        HotDealProductPagingResponseDto cachedResult = (HotDealProductPagingResponseDto)objectValueOperations.get(key);

        assertThat(cachedResult.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(cachedResult.getCursor()).isEqualTo(expectedCursor);
        List<HotDealProductResponseDto> cachedResultHotDealProducts = cachedResult.getHotDealProducts();
        assertThat(cachedResultHotDealProducts).hasSize(size);
    }

    @Test
    @Transactional
    @DisplayName("hotDealProducts 페이지 조회 및 cacheHit")
    public void getHotDealProducts_cacheHit(){
        //given
        String hotDealProductTitlePrefix = "product";
        List<HotDealProduct> hotDealProducts = createTestHotDealProducts(hotDealProductTitlePrefix);
        HotDeal hotDeal = createTestHotDeal(hotDealProducts);
        int size = 3;
        int hotDealProductSize = hotDeal.getHotDealProducts().size();
        Long expectedCursor = hotDeal.getHotDealProducts().get(hotDealProductSize - size).getId();
        Long cursor = null;
        String search = null;

        hotDealProductService.getHotDealProducts(hotDeal.getId(), null, null, size);
        ValueOperations<String, Object> objectValueOperations = redisTemplate.opsForValue();
        String key = gethotDealProductPagingKey(hotDeal.getId(), cursor, size, search);
        HotDealProductPagingResponseDto cachedResult = (HotDealProductPagingResponseDto)objectValueOperations.get(key);

        verify(hotDealProductRepository, times(1))
                .findByCursorAndSearchAndSizeHotDealProducts(eq(hotDeal.getId()), eq(Long.MAX_VALUE), eq(search), any(Pageable.class));
        assertThat(cachedResult.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(cachedResult.getCursor()).isEqualTo(expectedCursor);
        List<HotDealProductResponseDto> cachedResultHotDealProducts = cachedResult.getHotDealProducts();
        assertThat(cachedResultHotDealProducts).hasSize(size);

        //when
        HotDealProductPagingResponseDto result = hotDealProductService.getHotDealProducts(hotDeal.getId(), null, null, size);

        //then
        assertThat(result.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(result.getCursor()).isEqualTo(expectedCursor);
        List<HotDealProductResponseDto> resultHotDealProducts = result.getHotDealProducts();
        assertThat(resultHotDealProducts).hasSize(size);
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 단건 조회 및 cacheMiss 캐시 저장")
    public void getHotDealProduct_cacheMiss(){
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product");
        HotDeal hotDeal = createTestHotDeal(List.of(hotDealProduct));

        //when
        HotDealProductCacheDto result = hotDealProductService.getHotDealProduct(hotDealProduct.getId());

        //then
        verify(hotDealProductRepository, times(1)).findById(hotDealProduct.getId());
        assertThat(result.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(result.getHotDealProductId()).isEqualTo(hotDealProduct.getId());

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = getHotDealProductKey(hotDealProduct.getId());
        HotDealProductCacheDto cachedResult = (HotDealProductCacheDto) valueOperations.get(key);
        assertThat(cachedResult.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(cachedResult.getHotDealProductId()).isEqualTo(hotDealProduct.getId());
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 단건 조회 및 cacheHit")
    public void getHotDealProduct_cacheHit(){
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product");
        HotDeal hotDeal = createTestHotDeal(List.of(hotDealProduct));

        hotDealProductService.getHotDealProduct(hotDealProduct.getId());

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = getHotDealProductKey(hotDealProduct.getId());
        HotDealProductCacheDto cachedResult = (HotDealProductCacheDto) valueOperations.get(key);
        assertThat(cachedResult.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(cachedResult.getHotDealProductId()).isEqualTo(hotDealProduct.getId());
        verify(hotDealProductRepository, times(1)).findById(hotDealProduct.getId());

        //when
        HotDealProductCacheDto result = hotDealProductService.getHotDealProduct(hotDealProduct.getId());

        //then
        assertThat(result.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(result.getHotDealProductId()).isEqualTo(hotDealProduct.getId());
    }
}
