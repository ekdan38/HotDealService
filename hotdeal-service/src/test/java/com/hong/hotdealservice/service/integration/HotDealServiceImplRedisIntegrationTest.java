package com.hong.hotdealservice.service.integration;

import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.hotdealservice.client.Resilience4JProductServiceClient;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import com.hong.hotdealservice.dto.*;
import com.hong.hotdealservice.repository.HotDealProductRepository;
import com.hong.hotdealservice.repository.HotDealRedisRepository;
import com.hong.hotdealservice.repository.HotDealRepository;
import com.hong.hotdealservice.service.HotDealProductServiceImpl;
import com.hong.hotdealservice.service.HotDealServiceImpl;
import com.hong.hotdealservice.web.dto.HotDealProductRequestDto;
import com.hong.hotdealservice.web.dto.HotDealProductUpdateRequestDto;
import com.hong.hotdealservice.web.dto.HotDealRequestDto;
import com.hong.hotdealservice.web.dto.HotDealUpdateRequestDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
public class HotDealServiceImplRedisIntegrationTest {

    @Autowired
    HotDealServiceImpl hotDealService;
    @Autowired
    HotDealProductServiceImpl hotDealProductService;
    @MockitoSpyBean
    HotDealRepository hotDealRepository;
    @MockitoSpyBean
    HotDealProductRepository hotDealProductRepository;
    @MockitoBean
    Resilience4JProductServiceClient resilience4JProductServiceClient;
    @Autowired
    HotDealRedisRepository hotDealRedisRepository;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;
    @Autowired
    EntityManager em;

    private Long adminId = 1L;
    private int originalPrice = 10000;
    private double discountRate = 0.1;

    private HotDealProduct createTestHotDealProduct(Long productId, String productTitle){
        return HotDealProduct.create(productId, productTitle, originalPrice, discountRate, 100);
    }

    private HotDeal createTestHotDeal(Long adminId, String hotDealTitle, List<HotDealProduct> hp){
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal = HotDeal.create(adminId, hotDealTitle, "description", now, now.plusHours(1), hp);
        hotDealRepository.save(hotDeal);
        em.flush();
        em.clear();
        return hotDeal;
    }

    private List<HotDeal> createTestHotDeals(String titlePrefix){
        List<HotDeal> hotDeals = new ArrayList<>();
        for(long i = 1; i <= 5; i++){
            HotDeal hotDeal = createTestHotDeal(adminId, titlePrefix + i, List.of());
            hotDeals.add(hotDeal);
        }
        return hotDeals;
    }
    private String getHotDealKey(Long hotDealId){
        return  "getHotDeal::hot_deals:" + hotDealId;
    }
    private String getHotDealPagingKey(Long cursor, int size, String search){
        return "getHotDeals::hot_deals:cursor:" + (cursor == null ? "" : cursor)
                + ":size:" + size + ":search:" + (search == null ? "" : search);
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
    @DisplayName("hotDeal 페이징 조회 및 cacheMiss 발생 캐시 저장")
    public void getHotDeals_cacheMiss(){
        //given
        String titlePrefix = "product";
        List<HotDeal> hotDeals = createTestHotDeals(titlePrefix);
        Long expectedCursor = hotDeals.get(0).getId();

        Long cursor = null;
        String search = null;
        int size = 5;

        //when
        HotDealPagingCacheDto result = hotDealService.getHotDeals(search, cursor, size);

        //then
        assertThat(result.getCursor()).isEqualTo(expectedCursor);
        assertThat(result.getHotDeals()).hasSize(5);
        result.getHotDeals().forEach(pr -> {
            assertThat(pr.getTitle()).startsWith(titlePrefix);
            assertThat(pr.getAdminId()).isEqualTo(adminId);
        });

        // Redis 조회
        HotDealPagingCacheDto cachedResult = hotDealRedisRepository.findPagingByCursorAndSizeAndSearch(cursor, size, search);
        assertThat(cachedResult).isNotNull();
        assertThat(cachedResult.getCursor()).isEqualTo(expectedCursor);
        assertThat(cachedResult.getHotDeals()).hasSize(5);
        cachedResult.getHotDeals().forEach(pr -> {
            assertThat(pr.getTitle()).startsWith(titlePrefix);
            assertThat(pr.getAdminId()).isEqualTo(adminId);
        });
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 페이징 조회 및 cacheHit")
    public void getHotDeals_cacheHit(){
        //given
        String titlePrefix = "product";
        List<HotDeal> hotDeals = createTestHotDeals(titlePrefix);
        Long expectedCursor = hotDeals.get(0).getId();

        Long cursor = null;
        String search = null;
        int size = 5;
        // 페이징 실행(cache 저장) 및 검증
        executeGetHotDealsAndValidate(search, cursor, size, titlePrefix, expectedCursor);

        //when
        HotDealPagingCacheDto result = hotDealService.getHotDeals(search, cursor, size);

        //then
        verify(hotDealRepository, times(1)).findByCursorAndSearchAndSizeHotDeals(anyLong(), any(), any());
        assertThat(result.getCursor()).isEqualTo(expectedCursor);
        assertThat(result.getHotDeals()).hasSize(5);
        result.getHotDeals().forEach(pr -> {
            assertThat(pr.getTitle()).startsWith(titlePrefix);
            assertThat(pr.getAdminId()).isEqualTo(adminId);
        });
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 생성 및 getHotDeals 캐시 전면 무효화")
    public void getHotDeals_cacheEvict_getHotDeals(){
        //given
        String titlePrefix = "product";
        List<HotDeal> hotDeals = createTestHotDeals(titlePrefix);
        Long expectedCursor = hotDeals.get(0).getId();
        Long cursor = null;
        String search = null;
        int size = 5;
        // 페이징 실행(cache 저장) 및 검증
        executeGetHotDealsAndValidate(search, cursor, size, titlePrefix, expectedCursor);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);

        HotDealRequestDto requestDto = new HotDealRequestDto("creatHotDeal", "description",
                startTime, endTime, List.of(new HotDealProductRequestDto(1L, 100, 0.1)));

        when(resilience4JProductServiceClient.decreaseStock(anyList())).thenReturn(List.of(
                new ProductStockUpdateResponseDto(1L, "hotDealProduct", 1000, 100)));
        //when
        HotDealCacheDto result = hotDealService.createHotDeal(adminId, requestDto);

        //then
        String key = "getHotDeals::hot_deals:cursor*";
        Set<String> keys = redisTemplate.keys(key);
        assertThat(keys).isEmpty();
        assertThat(result.getHotDealId()).isNotNull();
        assertThat(result.getAdminId()).isEqualTo(adminId);
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 단건 조회 및 cacheMiss 후 cache 저장")
    public void getHotDeal_cacheMiss(){
        //given
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal", List.of());

        //when
        HotDealCacheDto result = hotDealService.getHotDeal(hotDeal.getId());

        //then
        assertThat(result.getHotDealId()).isEqualTo(hotDeal.getId());
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = getHotDealKey(hotDeal.getId());
        HotDealCacheDto cachedResult = (HotDealCacheDto) valueOperations.get(key);
        assertThat(cachedResult.getHotDealId());
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 단건 조회 및 cacheHit")
    public void getHotDeal_cacheHit(){
        //given
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal", List.of());
        excuteGetHotDealAndValidate(hotDeal);

        //when
        HotDealCacheDto result = hotDealService.getHotDeal(hotDeal.getId());

        //then
        assertThat(result.getHotDealId()).isEqualTo(hotDeal.getId());
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 수정 및 getHotDeals 전면 무효, getHotDeal, getHotDealProduct, getHotDealProducts 부분 무효화")
    public void updateHotDeal_cacheEvict(){
        //given
        HotDealProduct targetHotDealProduct = createTestHotDealProduct(1L, "hotDealProduct");
        String hotDealProductsTitlePrefix = "hotDealProduct";
        List<HotDealProduct> targetHotDealProducts = createTestHotDealProducts(hotDealProductsTitlePrefix);
        targetHotDealProducts.add(targetHotDealProduct);
        HotDeal targetHotDeal = createTestHotDeal(adminId, "hotDeal", targetHotDealProducts);
        // hotDeal 단건 조회(캐시 적재)
        excuteGetHotDealAndValidate(targetHotDeal);

        // hotDeal 페이징 조회(캐시 적재)
        String hotDealTitlePrefix = "hotDeal";
        Long cursor = 5000L;
        String search = "Deal";
        int size = 5;
        List<HotDeal> targetHotDeals = createTestHotDeals("hotDeal");
        Long expectedCursor = targetHotDeals.get(0).getId();
        executeGetHotDealsAndValidate(search, cursor, size, hotDealTitlePrefix, expectedCursor);

        // hotDealProducts 단건 조회(캐시 적재)
        excuteGetHotDealProductAndValidate(targetHotDealProduct);

        // hotDealProducts 페이징 조회(캐시 적재)
        Long hotDealId = targetHotDeal.getId();
        String productSearch = "Product";
        Long productCursor = 1000L;
        Long productExpectedCursor = targetHotDealProducts.get(1).getId();
        executeGetHotDealProductsAndValidate(hotDealId, productSearch, productCursor, size, hotDealProductsTitlePrefix, productExpectedCursor);

        // hotDeal(hotDealProducts) 수정
        String newHotDealTitle = "newTitle";
        String newDescription = "newDescription";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newStartTime = newNow.minusHours(1);
        LocalDateTime newEndTime = newNow.plusHours(1);
        HotDealStatus newStatus = HotDealStatus.EXPIRED;

        HotDealProductUpdateRequestDto hotDealProductUpdateRequestDto =
                new HotDealProductUpdateRequestDto(targetHotDealProduct.getId(), 1L, 1000, 0.8);

        HotDealUpdateRequestDto requestDto =
                new HotDealUpdateRequestDto(newHotDealTitle, newDescription, newStartTime, newEndTime, newStatus.name(), List.of(hotDealProductUpdateRequestDto));

        ProductStockUpdateResponseDto decreaseResponse =
                new ProductStockUpdateResponseDto(1L, "hotDealProduct", 2000, 900);

        when(resilience4JProductServiceClient.decreaseStock(anyList())).thenReturn(List.of(decreaseResponse));
        when(resilience4JProductServiceClient.increaseStock(anyList())).thenReturn(List.of(decreaseResponse));

        //when
        HotDealCacheDto result = hotDealService.updateHotDeal(hotDealId, requestDto);

        //then
        assertThat(result.getHotDealId()).isEqualTo(targetHotDeal.getId());
        assertThat(result.getTitle()).isEqualTo(newHotDealTitle);
        assertThat(result.getDescription()).isEqualTo(newDescription);
        assertThat(result.getStartTime()).isEqualTo(newStartTime);
        assertThat(result.getEndTime()).isEqualTo(newEndTime);
        assertThat(result.getStatus()).isEqualTo(newStatus.name());
        List<HotDealProductResponseDto> hotDealProducts = result.getHotDealProducts();
        assertThat(hotDealProducts).hasSize(1);
        assertThat(hotDealProducts.get(0).getStock()).isEqualTo(1000);
        assertThat(hotDealProducts.get(0).getDiscountRate()).isEqualTo(0.8);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        // hotDeal 단건 조회 캐시 무효화 확인
        String getHotDealKey = getHotDealKey(targetHotDeal.getId());
        assertThat(valueOperations.get(getHotDealKey)).isNull();

        // hotDeal 페이징 조회 캐시 무효화 확인
        String getHotDealskey = getHotDealPagingKey(cursor, size, search);
        assertThat(valueOperations.get(getHotDealskey)).isNull();

        // hotDealProduct 단건 조회 캐시 무효화 확인
        String getHotDealProductKey = getHotDealProductKey(targetHotDealProduct.getId());
        assertThat(valueOperations.get(getHotDealProductKey)).isNull();

        // hotDealProduct 페이징 조회 캐시 무효화 확인
        String getHotDealProductsKey = gethotDealProductPagingKey(hotDealId, productCursor, size, productSearch);
        assertThat(valueOperations.get(getHotDealProductsKey)).isNull();
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 삭제 및 getHotDeals 전면 무효, getHotDeal, getHotDealProduct, getHotDealProducts 부분 무효화")
    public void deleteHotDeal_cacheEvict(){
        //given
        HotDealProduct targetHotDealProduct = createTestHotDealProduct(1L, "hotDealProduct");
        String hotDealProductsTitlePrefix = "hotDealProduct";
        List<HotDealProduct> targetHotDealProducts = createTestHotDealProducts(hotDealProductsTitlePrefix);
        targetHotDealProducts.add(targetHotDealProduct);
        HotDeal targetHotDeal = createTestHotDeal(adminId, "hotDeal", targetHotDealProducts);
        // hotDeal 단건 조회(캐시 적재)
        excuteGetHotDealAndValidate(targetHotDeal);

        // hotDeal 페이징 조회(캐시 적재)
        String hotDealTitlePrefix = "hotDeal";
        Long cursor = 5000L;
        String search = "Deal";
        int size = 5;
        List<HotDeal> targetHotDeals = createTestHotDeals("hotDeal");
        Long expectedCursor = targetHotDeals.get(0).getId();
        executeGetHotDealsAndValidate(search, cursor, size, hotDealTitlePrefix, expectedCursor);

        // hotDealProducts 단건 조회(캐시 적재)
        excuteGetHotDealProductAndValidate(targetHotDealProduct);

        // hotDealProducts 페이징 조회(캐시 적재)
        Long hotDealId = targetHotDeal.getId();
        String productSearch = "Product";
        Long productCursor = 1000L;
        Long productExpectedCursor = targetHotDealProducts.get(1).getId();
        executeGetHotDealProductsAndValidate(hotDealId, productSearch, productCursor, size, hotDealProductsTitlePrefix, productExpectedCursor);

        ProductStockUpdateResponseDto increaseResponseDto =
                new ProductStockUpdateResponseDto(targetHotDealProduct.getProductId(), "title", 100, 100);

        when(resilience4JProductServiceClient.increaseStock(anyList())).thenReturn(List.of(increaseResponseDto));

        //when
        HotDealCacheDto result = hotDealService.deleteHotDeal(targetHotDeal.getId());

        //then
        assertThat(result.getHotDealId()).isEqualTo(targetHotDeal.getId());
        assertThat(result.getDeleted()).isTrue();
        result.getHotDealProducts().forEach(hp -> assertThat(hp.getStock()).isEqualTo(0));
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        // hotDeal 단건 조회 캐시 무효화 확인
        String getHotDealKey = getHotDealKey(targetHotDeal.getId());
        assertThat(valueOperations.get(getHotDealKey)).isNull();

        // hotDeal 페이징 조회 캐시 무효화 확인
        String getHotDealskey = getHotDealPagingKey(cursor, size, search);
        assertThat(valueOperations.get(getHotDealskey)).isNull();

        // hotDealProduct 단건 조회 캐시 무효화 확인
        String getHotDealProductKey = getHotDealProductKey(targetHotDealProduct.getId());
        assertThat(valueOperations.get(getHotDealProductKey)).isNull();

        // hotDealProduct 페이징 조회 캐시 무효화 확인
        String getHotDealProductsKey = gethotDealProductPagingKey(hotDealId, productCursor, size, productSearch);
        assertThat(valueOperations.get(getHotDealProductsKey)).isNull();
    }


    private List<HotDealProduct> createTestHotDealProducts(String titlePrefix){
        List<HotDealProduct> hotDealProducts = new ArrayList<>();
        for(long i = 1; i <= 5; i++){
            hotDealProducts.add(createTestHotDealProduct(i, "hotDealProduct" + i));
        }
        return hotDealProducts;
    }
    private void excuteGetHotDealProductAndValidate(HotDealProduct hotDealProduct){
        hotDealProductService.getHotDealProduct(hotDealProduct.getId());
        verify(hotDealProductRepository, times(1)).findById(anyLong());

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = "getHotDealProduct::hotdeal_products:" + hotDealProduct.getId();
        HotDealProductCacheDto cachedResult = (HotDealProductCacheDto) valueOperations.get(key);
        assertThat(cachedResult.getHotDealId());
    }

    private HotDealProductPagingResponseDto executeGetHotDealProductsAndValidate(Long hotDealId, String search, Long cursor, int size, String titlePrefix, Long expectedCursor) {
        HotDealProductPagingResponseDto hotDealProducts = hotDealProductService.getHotDealProducts(hotDealId, search, cursor, size);
        verify(hotDealProductRepository, times(1))
                .findByCursorAndSearchAndSizeHotDealProducts(eq(hotDealId), eq(cursor), eq(search), any());

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = gethotDealProductPagingKey(hotDealId, cursor, size, search);
        HotDealProductPagingResponseDto cachedResult = (HotDealProductPagingResponseDto) valueOperations.get(key);
        assertThat(cachedResult).isNotNull();
        assertThat(cachedResult.getCursor()).isEqualTo(expectedCursor);
        assertThat(cachedResult.getHotDealProducts()).hasSize(5);
        cachedResult.getHotDealProducts().forEach(pr -> {
            assertThat(pr.getProductTitle()).startsWith(titlePrefix);
        });
        return hotDealProducts;
    }

    private void excuteGetHotDealAndValidate(HotDeal hotDeal) {
        hotDealService.getHotDeal(hotDeal.getId());
        verify(hotDealRepository, times(1)).findById(anyLong());

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = "getHotDeal::hot_deals:" + hotDeal.getId();
        HotDealCacheDto cachedResult = (HotDealCacheDto) valueOperations.get(key);
        assertThat(cachedResult.getHotDealId());
    }

    private HotDealPagingCacheDto executeGetHotDealsAndValidate(String search, Long cursor, int size, String titlePrefix, Long expectedCursor) {
        HotDealPagingCacheDto result = hotDealService.getHotDeals(search, cursor, size);
        verify(hotDealRepository, times(1))
                .findByCursorAndSearchAndSizeHotDeals(any(Long.class), eq(search), any(Pageable.class));

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String key = getHotDealPagingKey(cursor, size, search);
        HotDealPagingCacheDto cachedResult = (HotDealPagingCacheDto) valueOperations.get(key);
        assertThat(cachedResult).isNotNull();
        assertThat(cachedResult.getCursor()).isEqualTo(expectedCursor);
        assertThat(cachedResult.getHotDeals()).hasSize(5);
        cachedResult.getHotDeals().forEach(pr -> {
            assertThat(pr.getTitle()).startsWith(titlePrefix);
            assertThat(pr.getAdminId()).isEqualTo(adminId);
        });
        return result;
    }




}
