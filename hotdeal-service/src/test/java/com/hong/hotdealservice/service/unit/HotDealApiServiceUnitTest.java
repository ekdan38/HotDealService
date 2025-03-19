package com.hong.hotdealservice.service.unit;

import com.hong.common.dto.HotDealProductStockCheckRequestDto;
import com.hong.common.dto.HotDealProductStockCheckResponseDto;
import com.hong.common.dto.HotDealProductStockUpdateRequestDto;
import com.hong.common.dto.HotDealProductStockUpdateResponseDto;
import com.hong.common.exception.custom.HotDealException;
import com.hong.common.exception.custom.HotDealProductException;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.HotDealProductCacheDto;
import com.hong.hotdealservice.dto.HotDealProductStockProjection;
import com.hong.hotdealservice.dto.HotDealCacheDto;
import com.hong.hotdealservice.repository.HotDealProductRedisRepository;
import com.hong.hotdealservice.repository.HotDealProductRepository;
import com.hong.hotdealservice.repository.HotDealRedisRepository;
import com.hong.hotdealservice.repository.HotDealRepository;
import com.hong.hotdealservice.service.HotDealApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotDealApiServiceUnitTest {

    @InjectMocks
    HotDealApiService hotDealApiService;
    @Mock
    HotDealProductRepository hotDealProductRepository;
    @Mock
    HotDealRepository hotDealRepository;
    @Mock
    RedissonClient redissonClient;
    @Mock
    HotDealProductRedisRepository hotDealProductRedisRepository;
    @Mock
    HotDealRedisRepository hotDealRedisRepository;

    private HotDealProduct createTestHotDealProduct(Long productId, String productTitle, int stock){
        return HotDealProduct.create(productId, productTitle, 1000, 0.1, stock);
    }

    private HotDeal createTestHotDeal(String title, LocalDateTime startTime, List<HotDealProduct> hp){
        LocalDateTime endTime = startTime.plusDays(1);
        return HotDeal.create(1L, title, "description", startTime, endTime, hp);
    }

    private void setUpForFetchHotDealProducts(int stock,
                                              List<HotDealProduct> hotDealProducts,
                                              List<HotDealProduct> hotDeal1Products,
                                              List<HotDealProduct> hotDeal2Products){
        for(long i = 1; i <= 4; i++){
            HotDealProduct hotDealProduct = createTestHotDealProduct(i, "product" + i, stock);
            ReflectionTestUtils.setField(hotDealProduct, "id", i);
            if(hotDeal1Products != null && i < 3) hotDeal1Products.add(hotDealProduct);
            if (hotDeal2Products != null && i >= 3) hotDeal2Products.add(hotDealProduct);
            hotDealProducts.add(hotDealProduct);
        }
    }

    @Test
    @DisplayName("hotDealProducts 재고 조회_성공")
    public void fetchStock_success(){
        //given
        int stock = 100;
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        setUpForFetchHotDealProducts(stock, hotDealProducts, null, null);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        createTestHotDeal("hotDeal1", startTime, hotDealProducts);

        List<Long> hotDealProductsIds = hotDealProducts.stream().map(hp -> hp.getId()).toList();
        List<HotDealProductStockProjection> stockProjections = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockProjection(hp.getId(), hp.getStock()))
                .toList();
        when(hotDealProductRepository.findStockByProductIds(hotDealProductsIds)).thenReturn(stockProjections);

        //when
        List<HotDealProductStockProjection> result = hotDealApiService.fetchStock(hotDealProductsIds);

        //then
        assertThat(result).hasSize(hotDealProductsIds.size());
        result.forEach(r -> {
            assertThat(r.getId()).isNotNull();
            assertThat(r.getStock()).isEqualTo(stock);
        });
        assertThat(result).hasSize(hotDealProductsIds.size());
    }

    @Test
    @DisplayName("hotDealProducts 재고 조회_실패")
    public void fetchStock_failure_notFoundHotDealProduct(){
        //given
        int stock = 100;
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        setUpForFetchHotDealProducts(stock, hotDealProducts, null, null);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        createTestHotDeal("hotDeal1", startTime, hotDealProducts);

        List<Long> hotDealProductsIds = hotDealProducts.stream().map(hp -> hp.getId()).toList();
        List<HotDealProductStockProjection> stockProjections = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockProjection(hp.getId(), hp.getStock()))
                .toList();
        when(hotDealProductRepository.findStockByProductIds(hotDealProductsIds)).thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> hotDealApiService.fetchStock(hotDealProductsIds)).isInstanceOf(HotDealProductException.class);
    }

    @Test
    @DisplayName("hotDealProducts 조회 및 재고 검증_성공_hotDealProduct, hotDeal_cacheMiss")
    public void fetchHotDealProductsStockAndValidateStock_success_hotDealProduct_hotDeal_cacheMiss(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal1Products = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal2Products = new ArrayList<>();
        setUpForFetchHotDealProducts(100, hotDealProducts, hotDeal1Products, hotDeal2Products);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal1 = createTestHotDeal("hotDeal1", startTime, hotDeal1Products);
        ReflectionTestUtils.setField(hotDeal1, "id", 1L);
        HotDeal hotDeal2 = createTestHotDeal("hotDeal2", startTime, hotDeal2Products);
        ReflectionTestUtils.setField(hotDeal2, "id", 2L);

        int requestedQuantity = 10;
        // request
        List<HotDealProductStockCheckRequestDto> requestDtos = hotDealProducts.stream()
                .map(hp -> new HotDealProductStockCheckRequestDto(hp.getProductId(), requestedQuantity))
                .toList();

        // hotDealProducts Redis 조회 => 전체 cacheMiss
        List<Long> requestHotDealProductIds = requestDtos.stream().map(r -> r.getHotDealProductId()).toList();
        List<HotDealProductCacheDto> cachedHotDealProducts = requestDtos.stream().map(key -> (HotDealProductCacheDto) null).toList();
        when(hotDealProductRedisRepository.findAllHotDealProductByIds(requestHotDealProductIds)).thenReturn(cachedHotDealProducts);

        // cacheMiss hotDealProducts DB 조회
        List<Long> hotDealProductIds = requestDtos.stream()
                .map(dto -> dto.getHotDealProductId())
                .toList();
        when(hotDealProductRepository.findByIds(hotDealProductIds)).thenReturn(hotDealProducts);

        // hotDeal Redis 조회 => 전체 cacheMiss
        List<Long> hotDealIds = List.of(hotDeal1.getId(), hotDeal2.getId());
        List<HotDealCacheDto> cacheHotDeals = hotDealIds.stream().map(key ->  (HotDealCacheDto)null).toList();
        when(hotDealRedisRepository.findAllHotDealByIds(hotDealIds)).thenReturn(cacheHotDeals);

        // cacheMiss hotDeal DB 조회
        when(hotDealRepository.findByIds(List.of(hotDeal1.getId(), hotDeal2.getId()))).thenReturn(List.of(hotDeal1, hotDeal2));

        //when
        List<HotDealProductStockCheckResponseDto> result = hotDealApiService.fetchHotDealProductsStockAndValidateStock(requestDtos);

        //then
        assertFetchHotDealProductStockAndValidateStock(result, hotDeal1, hotDeal2, requestedQuantity);
    }

    @Test
    @DisplayName("hotDealProducts 조회 및 재고 검증_성공_hotDealProduct, hotDeal_부분_cacheMiss")
    public void fetchHotDealProductsStockAndValidateStock_success_hotDealProduct_hotDeal_part_cacheMiss(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal1Products = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal2Products = new ArrayList<>();
        setUpForFetchHotDealProducts(100, hotDealProducts, hotDeal1Products, hotDeal2Products);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal1 = createTestHotDeal("hotDeal1", startTime, hotDeal1Products);
        ReflectionTestUtils.setField(hotDeal1, "id", 1L);
        HotDeal hotDeal2 = createTestHotDeal("hotDeal2", startTime, hotDeal2Products);
        ReflectionTestUtils.setField(hotDeal2, "id", 2L);

        int requestedQuantity = 10;
        // request
        List<HotDealProductStockCheckRequestDto> requestDtos = hotDealProducts.stream()
                .map(hp -> new HotDealProductStockCheckRequestDto(hp.getProductId(), requestedQuantity))
                .toList();

        // hotDealProducts Redis 조회 => 부분 cacheMiss
        List<HotDealProductCacheDto> cachedHotDealProducts = hotDeal1Products.stream().map(HotDealProductCacheDto::new).collect(Collectors.toList());
        cachedHotDealProducts.add(null);
        cachedHotDealProducts.add(null);
        List<Long> requestHotDealProductIds = requestDtos.stream().map(r -> r.getHotDealProductId()).toList();
        when(hotDealProductRedisRepository.findAllHotDealProductByIds(requestHotDealProductIds)).thenReturn(cachedHotDealProducts);


        // cacheMiss hotDealProducts DB 조회
        List<Long> hotDealProductIds = hotDeal2Products.stream()
                .map(hp -> hp.getId())
                .toList();
        when(hotDealProductRepository.findByIds(hotDealProductIds)).thenReturn(hotDeal2Products);

        // hotDeal Redis 조회 => 부분 cacheMiss
        List<Long> hotDealIds = List.of(hotDeal1.getId(), hotDeal2.getId());
        List<HotDealCacheDto> cacheHotDeals = new ArrayList<>();
        cacheHotDeals.add(new HotDealCacheDto(hotDeal1));
        cacheHotDeals.add(null);
        when(hotDealRedisRepository.findAllHotDealByIds(hotDealIds)).thenReturn(cacheHotDeals);

        // cacheMiss hotDeal DB 조회
        when(hotDealRepository.findByIds(List.of(hotDeal2.getId()))).thenReturn(List.of(hotDeal2));

        List<Long> cacheHitHotDealProductIds = hotDeal1Products.stream().map(hp -> hp.getId()).toList();
        List<HotDealProductStockProjection> stockProjections = hotDeal1Products
                .stream()
                .map(hp -> new HotDealProductStockProjection(hp.getId(), hp.getStock()))
                .toList();
        when(hotDealProductRepository.findStockByProductIds(cacheHitHotDealProductIds)).thenReturn(stockProjections);

        //when
        List<HotDealProductStockCheckResponseDto> result = hotDealApiService.fetchHotDealProductsStockAndValidateStock(requestDtos);

        //then
        assertFetchHotDealProductStockAndValidateStock(result, hotDeal1, hotDeal2, requestedQuantity);
    }

    @Test
    @DisplayName("hotDealProducts 조회 및 재고 검증_성공_cacheHit")
    public void fetchHotDealProductsStockAndValidateStock_success_cacheHit(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal1Products = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal2Products = new ArrayList<>();
        setUpForFetchHotDealProducts(100, hotDealProducts, hotDeal1Products, hotDeal2Products);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal1 = createTestHotDeal("hotDeal1", startTime, hotDeal1Products);
        ReflectionTestUtils.setField(hotDeal1, "id", 1L);
        HotDeal hotDeal2 = createTestHotDeal("hotDeal2", startTime, hotDeal2Products);
        ReflectionTestUtils.setField(hotDeal2, "id", 2L);

        int requestedQuantity = 10;
        // request
        List<HotDealProductStockCheckRequestDto> requestDtos = hotDealProducts.stream()
                .map(hp -> new HotDealProductStockCheckRequestDto(hp.getProductId(), requestedQuantity))
                .toList();

        // hotDealProducts Redis 조회 => 전체 cacheHit
        List<Long> requestHotDealProductIds = requestDtos.stream().map(r -> r.getHotDealProductId()).toList();
        List<HotDealProductCacheDto> cachedHotDealProducts = hotDealProducts.stream().map(HotDealProductCacheDto::new).collect(Collectors.toList());
        when(hotDealProductRedisRepository.findAllHotDealProductByIds(requestHotDealProductIds)).thenReturn(cachedHotDealProducts);

        // hotDeal Redis 조회 => 전체 cacheHit
        List<Long> hotDealIds = List.of(hotDeal1.getId(), hotDeal2.getId());
        List<HotDealCacheDto> cacheHotDeals = new ArrayList<>();
        cacheHotDeals.add(new HotDealCacheDto(hotDeal1));
        cacheHotDeals.add(new HotDealCacheDto(hotDeal2));
        when(hotDealRedisRepository.findAllHotDealByIds(hotDealIds)).thenReturn(cacheHotDeals);

        List<Long> cacheHitHotDealProductIds = hotDealProducts.stream().map(hp -> hp.getId()).toList();
        List<HotDealProductStockProjection> stockProjections = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockProjection(hp.getId(), hp.getStock()))
                .toList();
        when(hotDealProductRepository.findStockByProductIds(cacheHitHotDealProductIds)).thenReturn(stockProjections);

        //when
        List<HotDealProductStockCheckResponseDto> result = hotDealApiService.fetchHotDealProductsStockAndValidateStock(requestDtos);

        //then
        assertFetchHotDealProductStockAndValidateStock(result, hotDeal1, hotDeal2, requestedQuantity);
    }

    @Test
    @DisplayName("hotDealProducts 조회 및 재고 검증_실패_존재 하지 않는 hotDealProduct")
    public void fetchHotDealProductsStockAndValidateStock_failure_notFoundHotDealProduct(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal1Products = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal2Products = new ArrayList<>();
        setUpForFetchHotDealProducts(100, hotDealProducts, hotDeal1Products, hotDeal2Products);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal1 = createTestHotDeal("hotDeal1", startTime, hotDeal1Products);
        ReflectionTestUtils.setField(hotDeal1, "id", 1L);
        HotDeal hotDeal2 = createTestHotDeal("hotDeal2", startTime, hotDeal2Products);
        ReflectionTestUtils.setField(hotDeal2, "id", 2L);

        int requestedQuantity = 10;
        // request
        List<HotDealProductStockCheckRequestDto> requestDtos = hotDealProducts.stream()
                .map(hp -> new HotDealProductStockCheckRequestDto(hp.getProductId(), requestedQuantity))
                .toList();

        // hotDealProducts Redis 조회 => 부분 cacheMiss
        List<Long> requestHotDealProductIds = requestDtos.stream().map(r -> r.getHotDealProductId()).toList();
        List<HotDealProductCacheDto> cachedHotDealProducts = hotDeal1Products.stream().map(HotDealProductCacheDto::new).collect(Collectors.toList());
        cachedHotDealProducts.add(null);
        cachedHotDealProducts.add(null);
        when(hotDealProductRedisRepository.findAllHotDealProductByIds(requestHotDealProductIds)).thenReturn(cachedHotDealProducts);

        // cacheMiss hotDealProducts DB 조회
        List<Long> hotDealProductIds = hotDeal2Products.stream()
                .map(hp -> hp.getId())
                .toList();
        when(hotDealProductRepository.findByIds(hotDealProductIds)).thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> hotDealApiService.fetchHotDealProductsStockAndValidateStock(requestDtos)).isInstanceOf(HotDealProductException.class);
    }

    @Test
    @DisplayName("hotDealProducts 조회 및 재고 검증_실패_존재 하지 않는 hotDeal")
    public void fetchHotDealProductsStockAndValidateStock_failure_notFoundHotDeal(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal1Products = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal2Products = new ArrayList<>();
        setUpForFetchHotDealProducts(100, hotDealProducts, hotDeal1Products, hotDeal2Products);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal1 = createTestHotDeal("hotDeal1", startTime, hotDeal1Products);
        ReflectionTestUtils.setField(hotDeal1, "id", 1L);
        HotDeal hotDeal2 = createTestHotDeal("hotDeal2", startTime, hotDeal2Products);
        ReflectionTestUtils.setField(hotDeal2, "id", 2L);

        int requestedQuantity = 10;
        // request
        List<HotDealProductStockCheckRequestDto> requestDtos = hotDealProducts.stream()
                .map(hp -> new HotDealProductStockCheckRequestDto(hp.getProductId(), requestedQuantity))
                .toList();

        // hotDealProducts Redis 조회 => 부분 cacheMiss
        List<HotDealProductCacheDto> cachedHotDealProducts = hotDeal1Products.stream().map(HotDealProductCacheDto::new).collect(Collectors.toList());
        cachedHotDealProducts.add(null);
        cachedHotDealProducts.add(null);
        List<Long> requestHotDealProductIds = requestDtos.stream().map(r -> r.getHotDealProductId()).toList();
        when(hotDealProductRedisRepository.findAllHotDealProductByIds(requestHotDealProductIds)).thenReturn(cachedHotDealProducts);

        // cacheMiss hotDealProducts DB 조회
        List<Long> hotDealProductIds = hotDeal2Products.stream()
                .map(hp -> hp.getId())
                .toList();
        when(hotDealProductRepository.findByIds(hotDealProductIds)).thenReturn(hotDeal2Products);

        // hotDeal Redis 조회 => 부분 cacheMiss
        List<Long> hotDealIds = List.of(hotDeal1.getId(), hotDeal2.getId());
        List<HotDealCacheDto> cacheHotDeals = new ArrayList<>();
        cacheHotDeals.add(new HotDealCacheDto(hotDeal1));
        cacheHotDeals.add(null);
        when(hotDealRedisRepository.findAllHotDealByIds(hotDealIds)).thenReturn(cacheHotDeals);

        // cacheMiss hotDeal DB 조회
        when(hotDealRepository.findByIds(List.of(hotDeal2.getId()))).thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> hotDealApiService.fetchHotDealProductsStockAndValidateStock(requestDtos)).isInstanceOf(HotDealException.class);
    }

    @Test
    @DisplayName("hotDealProducts 조회 및 재고 검증_실패_주문 가능 상태가 아닌 hotDeal")
    public void fetchHotDealProductsStockAndValidateStock_failure_nonActive(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal1Products = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal2Products = new ArrayList<>();
        setUpForFetchHotDealProducts(100, hotDealProducts, hotDeal1Products, hotDeal2Products);

        LocalDateTime startTime = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal1 = createTestHotDeal("hotDeal1", startTime, hotDeal1Products);
        ReflectionTestUtils.setField(hotDeal1, "id", 1L);
        HotDeal hotDeal2 = createTestHotDeal("hotDeal2", startTime, hotDeal2Products);
        ReflectionTestUtils.setField(hotDeal2, "id", 2L);

        int requestedQuantity = 10;
        // 요청 Dto
        List<HotDealProductStockCheckRequestDto> requestDtos = hotDealProducts.stream()
                .map(hp -> new HotDealProductStockCheckRequestDto(hp.getProductId(), requestedQuantity))
                .toList();

        // hotDealProducts Redis 조회 => 부분 cacheMiss
        List<Long> requestHotDealProductIds = requestDtos.stream().map(r -> r.getHotDealProductId()).toList();
        List<HotDealProductCacheDto> cachedHotDealProducts = hotDeal1Products.stream().map(HotDealProductCacheDto::new).collect(Collectors.toList());
        cachedHotDealProducts.add(null);
        cachedHotDealProducts.add(null);
        when(hotDealProductRedisRepository.findAllHotDealProductByIds(requestHotDealProductIds)).thenReturn(cachedHotDealProducts);

        // cacheMiss hotDealProducts DB 조회
        List<Long> hotDealProductIds = hotDeal2Products.stream()
                .map(hp -> hp.getId())
                .toList();
        when(hotDealProductRepository.findByIds(hotDealProductIds)).thenReturn(hotDeal2Products);

        // hotDeal Redis 조회 => 부분 cacheMiss
        List<Long> hotDealIds = List.of(hotDeal1.getId(), hotDeal2.getId());
        List<HotDealCacheDto> cacheHotDeals = new ArrayList<>();
        cacheHotDeals.add(new HotDealCacheDto(hotDeal1));
        cacheHotDeals.add(null);
        when(hotDealRedisRepository.findAllHotDealByIds(hotDealIds)).thenReturn(cacheHotDeals);

        // cacheMiss hotDeal DB 조회
        when(hotDealRepository.findByIds(List.of(hotDeal2.getId()))).thenReturn(List.of(hotDeal2));

        //when && then
        assertThatThrownBy(() -> hotDealApiService.fetchHotDealProductsStockAndValidateStock(requestDtos)).isInstanceOf(HotDealException.class);
    }

    @Test
    @DisplayName("hotDealProducts 조회 및 재고 검증_실패_재고 조회_존재 하지 않는 hotDealProduct")
    public void fetchHotDealProductsStockAndValidateStock_failure_fetchStock_notFoundHotDealProduct(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal1Products = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal2Products = new ArrayList<>();
        setUpForFetchHotDealProducts(100, hotDealProducts, hotDeal1Products, hotDeal2Products);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal1 = createTestHotDeal("hotDeal1", startTime, hotDeal1Products);
        ReflectionTestUtils.setField(hotDeal1, "id", 1L);
        HotDeal hotDeal2 = createTestHotDeal("hotDeal2", startTime, hotDeal2Products);
        ReflectionTestUtils.setField(hotDeal2, "id", 2L);

        int requestedQuantity = 10;
        // request
        List<HotDealProductStockCheckRequestDto> requestDtos = hotDealProducts.stream()
                .map(hp -> new HotDealProductStockCheckRequestDto(hp.getProductId(), requestedQuantity))
                .toList();

        // hotDealProducts Redis 조회 => 부분 cacheMiss
        List<Long> requestHotDealProductIds = requestDtos.stream().map(r -> r.getHotDealProductId()).toList();
        List<HotDealProductCacheDto> cachedHotDealProducts = hotDeal1Products.stream().map(HotDealProductCacheDto::new).collect(Collectors.toList());
        cachedHotDealProducts.add(null);
        cachedHotDealProducts.add(null);
        when(hotDealProductRedisRepository.findAllHotDealProductByIds(requestHotDealProductIds)).thenReturn(cachedHotDealProducts);

        // cacheMiss hotDealProducts DB 조회
        List<Long> hotDealProductIds = hotDeal2Products.stream()
                .map(hp -> hp.getId())
                .toList();
        when(hotDealProductRepository.findByIds(hotDealProductIds)).thenReturn(hotDeal2Products);

        // hotDeal Redis 조회 => 부분 cacheMiss
        List<Long> hotDealIds = List.of(hotDeal1.getId(), hotDeal2.getId());
        List<HotDealCacheDto> cacheHotDeals = new ArrayList<>();
        cacheHotDeals.add(new HotDealCacheDto(hotDeal1));
        cacheHotDeals.add(null);
        when(hotDealRedisRepository.findAllHotDealByIds(hotDealIds)).thenReturn(cacheHotDeals);

        // cacheMiss hotDeal DB 조회
        when(hotDealRepository.findByIds(List.of(hotDeal2.getId()))).thenReturn(List.of(hotDeal2));

        List<Long> cacheHitHotDealProductIds = hotDeal1Products.stream().map(hp -> hp.getId()).toList();
        when(hotDealProductRepository.findStockByProductIds(cacheHitHotDealProductIds)).thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> hotDealApiService.fetchHotDealProductsStockAndValidateStock(requestDtos)).isInstanceOf(HotDealProductException.class);
    }

    @Test
    @DisplayName("hotDealProducts 조회 및 재고 검증_실패_요청 수량 보다 재고 부족")
    public void fetchHotDealProductsStockAndValidateStock_failure_notEnoughStock(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal1Products = new ArrayList<>();
        ArrayList<HotDealProduct> hotDeal2Products = new ArrayList<>();
        setUpForFetchHotDealProducts(100, hotDealProducts, hotDeal1Products, hotDeal2Products);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal1 = createTestHotDeal("hotDeal1", startTime, hotDeal1Products);
        ReflectionTestUtils.setField(hotDeal1, "id", 1L);
        HotDeal hotDeal2 = createTestHotDeal("hotDeal2", startTime, hotDeal2Products);
        ReflectionTestUtils.setField(hotDeal2, "id", 2L);

        int requestedQuantity = 110;
        // request
        List<HotDealProductStockCheckRequestDto> requestDtos = hotDealProducts.stream()
                .map(hp -> new HotDealProductStockCheckRequestDto(hp.getProductId(), requestedQuantity))
                .toList();

        // hotDealProducts Redis 조회 => 전체 cacheHit
        List<Long> requestHotDealProductIds = requestDtos.stream().map(r -> r.getHotDealProductId()).toList();
        List<HotDealProductCacheDto> cachedHotDealProducts = hotDealProducts.stream().map(HotDealProductCacheDto::new).collect(Collectors.toList());
        when(hotDealProductRedisRepository.findAllHotDealProductByIds(requestHotDealProductIds)).thenReturn(cachedHotDealProducts);

        // hotDeal Redis 조회 => 전체 cacheHit
        List<Long> hotDealIds = List.of(hotDeal1.getId(), hotDeal2.getId());
        List<HotDealCacheDto> cacheHotDeals = new ArrayList<>();
        cacheHotDeals.add(new HotDealCacheDto(hotDeal1));
        cacheHotDeals.add(new HotDealCacheDto(hotDeal2));
        when(hotDealRedisRepository.findAllHotDealByIds(hotDealIds)).thenReturn(cacheHotDeals);

        List<Long> cacheHitHotDealProductIds = hotDealProducts.stream().map(hp -> hp.getId()).toList();
        List<HotDealProductStockProjection> stockProjections = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockProjection(hp.getId(), hp.getStock()))
                .toList();
        when(hotDealProductRepository.findStockByProductIds(cacheHitHotDealProductIds)).thenReturn(stockProjections);

        //when && then
        assertThatThrownBy(() -> hotDealApiService.fetchHotDealProductsStockAndValidateStock(requestDtos)).isInstanceOf(HotDealProductException.class);
    }


    @Test
    @DisplayName("hotDealProduct 재고 감소_성공")
    public void decreaseStock_success() throws InterruptedException {
        //given
        TransactionSynchronizationManager.initSynchronization();
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        int stock = 100;
        setUpForFetchHotDealProducts(stock, hotDealProducts, null, null);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal = createTestHotDeal("hotDeal1", startTime, hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);

        // lock mock 처리
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        List<Long> hotDealProductIds = hotDealProducts.stream().map(hp -> hp.getId()).toList();
        when(hotDealProductRepository.findByIds(hotDealProductIds)).thenReturn(hotDealProducts);

        int requestQuantity = 10;

        // request
        List<HotDealProductStockUpdateRequestDto> requestDtos = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockUpdateRequestDto(
                        hp.getId(), requestQuantity))
                .toList();

        //when
        List<HotDealProductStockUpdateResponseDto> result = hotDealApiService.decreaseStock(requestDtos);

        //then
        assertThat(result).hasSize(requestDtos.size());
        result.forEach(r -> {
            assertThat(r.getHotDealProductId()).isNotNull();
            assertThat(r.getTitle()).contains("product");
            assertThat(r.getRequestedQuantity()).isEqualTo(requestQuantity);
        });
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("hotDealProduct 재고 감소_실패_존재 하지 않는 hotDealProduct")
    public void decreaseStock_failure_notFoundHotDealProduct() throws InterruptedException {
        //given
        TransactionSynchronizationManager.initSynchronization();
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        int stock = 100;
        setUpForFetchHotDealProducts(stock, hotDealProducts, null, null);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal = createTestHotDeal("hotDeal1", startTime, hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);

        // lock mock 처리
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        List<Long> hotDealProductIds = hotDealProducts.stream().map(hp -> hp.getId()).toList();
        when(hotDealProductRepository.findByIds(hotDealProductIds)).thenReturn(List.of());

        int requestQuantity = 10;

        // request
        List<HotDealProductStockUpdateRequestDto> requestDtos = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockUpdateRequestDto(
                        hp.getId(), requestQuantity))
                .toList();

        //when && then
        assertThatThrownBy(() -> hotDealApiService.decreaseStock(requestDtos)).isInstanceOf(HotDealProductException.class);
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("hotDealProduct 재고 감소_실패_재고 부족")
    public void decreaseStock_failure_notEnoughStock() throws InterruptedException {
        //given
        TransactionSynchronizationManager.initSynchronization();
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        int stock = 100;
        setUpForFetchHotDealProducts(stock, hotDealProducts, null, null);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal = createTestHotDeal("hotDeal1", startTime, hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);

        // lock mock 처리
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        List<Long> hotDealProductIds = hotDealProducts.stream().map(hp -> hp.getId()).toList();
        when(hotDealProductRepository.findByIds(hotDealProductIds)).thenReturn(List.of());

        int requestQuantity = 1000;

        // request
        List<HotDealProductStockUpdateRequestDto> requestDtos = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockUpdateRequestDto(
                        hp.getId(), requestQuantity))
                .toList();

        //when && then
        assertThatThrownBy(() -> hotDealApiService.decreaseStock(requestDtos)).isInstanceOf(HotDealProductException.class);
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("hotDealProduct 재고 증가_성공")
    public void increaseStock_success() throws InterruptedException {
        //given
        TransactionSynchronizationManager.initSynchronization();
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        int stock = 100;
        setUpForFetchHotDealProducts(stock, hotDealProducts, null, null);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal = createTestHotDeal("hotDeal1", startTime, hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);

        // lock mock 처리
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        List<Long> hotDealProductIds = hotDealProducts.stream().map(hp -> hp.getId()).toList();
        when(hotDealProductRepository.findByIds(hotDealProductIds)).thenReturn(hotDealProducts);

        int requestQuantity = 10;

        // request
        List<HotDealProductStockUpdateRequestDto> requestDtos = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockUpdateRequestDto(
                        hp.getId(), requestQuantity))
                .toList();

        //when
        List<HotDealProductStockUpdateResponseDto> result = hotDealApiService.increaseStock(requestDtos);

        //then
        assertThat(result).hasSize(requestDtos.size());
        result.forEach(r -> {
            assertThat(r.getHotDealProductId()).isNotNull();
            assertThat(r.getTitle()).contains("product");
            assertThat(r.getRequestedQuantity()).isEqualTo(requestQuantity);
        });
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("hotDealProduct 재고 증가_실패_존재 하지 않는 hotDealProduct")
    public void increaseStock_failure_notFoundHotDealProduct() throws InterruptedException {
        //given
        TransactionSynchronizationManager.initSynchronization();
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        int stock = 100;
        setUpForFetchHotDealProducts(stock, hotDealProducts, null, null);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HotDeal hotDeal = createTestHotDeal("hotDeal1", startTime, hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);

        // lock mock 처리
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        List<Long> hotDealProductIds = hotDealProducts.stream().map(hp -> hp.getId()).toList();
        when(hotDealProductRepository.findByIds(hotDealProductIds)).thenReturn(List.of());

        int requestQuantity = 10;

        // request
        List<HotDealProductStockUpdateRequestDto> requestDtos = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockUpdateRequestDto(
                        hp.getId(), requestQuantity))
                .toList();

        //when && then
        assertThatThrownBy(() -> hotDealApiService.increaseStock(requestDtos)).isInstanceOf(HotDealProductException.class);
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void assertFetchHotDealProductStockAndValidateStock(List<HotDealProductStockCheckResponseDto> result, HotDeal hotDeal1, HotDeal hotDeal2, int requestedQuantity) {
        assertThat(result).hasSize(4);
        for(long i = 1; i <= 4; i++){
            HotDealProductStockCheckResponseDto resultDto = result.get((int)i - 1);
            assertThat(resultDto.getProductTitle()).isEqualTo("product" + i);
            assertThat(resultDto.getRequestedQuantity()).isEqualTo(requestedQuantity);
            assertThat(resultDto.getHotDealPrice()).isEqualTo(900);
        }
    }
}