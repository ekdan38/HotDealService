package com.hong.hotdealservice.service.unit;

import com.hong.common.exception.custom.HotDealProductException;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.HotDealProductCacheDto;
import com.hong.hotdealservice.dto.HotDealProductPagingResponseDto;
import com.hong.hotdealservice.dto.HotDealProductResponseDto;
import com.hong.hotdealservice.repository.HotDealProductRepository;
import com.hong.hotdealservice.service.HotDealProductServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotDealProductServiceImplUnitTest {

    @InjectMocks
    HotDealProductServiceImpl hotDealProductService;
    @Mock
    HotDealProductRepository hotDealProductRepository;

    private HotDealProduct createTestHotDealProduct(Long productId, String productTitle,
                                                    int originalPrice, double discountRate, int stock){
        return HotDealProduct.create(productId, productTitle, originalPrice, discountRate, stock);
    }

    private HotDeal createTestHotDeal(List<HotDealProduct> hp){
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime startTime = now.minusDays(1);
        LocalDateTime endTime = now.plusDays(1);
        return HotDeal.create(1L, "hotDeal", "description", startTime, endTime, hp);
    }

    @Test
    @DisplayName("hotDealProduct 페이징 조회_성공_search 포함")
    public void getHotDealProducts_success_wistSearch(){
        //given
        int originalPrice = 1000;
        double discountRate = 0.1;
        int stock = 100;
        int hotDealPrice = (int) Math.floor(originalPrice * (1 - discountRate));

        ArrayList<HotDealProduct> expectedHotDealProducts = new ArrayList<>();
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct;
        for(long i = 1; i <= 10; i++){
            if(i % 2 == 0) hotDealProduct = createTestHotDealProduct(i, "evenProduct" + i, originalPrice, discountRate, stock);
            else hotDealProduct = createTestHotDealProduct(i, "oddProduct" + i, originalPrice, discountRate, stock);
            ReflectionTestUtils.setField(hotDealProduct, "id", i);
            if (i % 2 == 0 && i > 5) expectedHotDealProducts.add(hotDealProduct);
            hotDealProducts.add(hotDealProduct);
        }
        List<HotDealProduct> reversedExpectedHotDealProducts = expectedHotDealProducts.reversed();

        HotDeal hotDeal = createTestHotDeal(hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);

        Long cursor = 10L;
        String search = "even";
        int size = 3;
        Long expectedCursor = 6L;

        when(hotDealProductRepository.findByCursorAndSearchAndSizeHotDealProducts(
                eq(hotDeal.getId()), eq(cursor), eq(search), any(Pageable.class)))
                .thenReturn(reversedExpectedHotDealProducts);
        //when
        HotDealProductPagingResponseDto result = hotDealProductService.getHotDealProducts(hotDeal.getId(), search, cursor, size);

        //then
        assertThat(result.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(result.getCursor()).isEqualTo(expectedCursor);
        List<HotDealProductResponseDto> resultHotDealProducts = result.getHotDealProducts();
        assertThat(resultHotDealProducts).hasSize(size);
        resultHotDealProducts.forEach(hp -> {
            assertThat(hp.getOriginalProductId()).isNotNull();
            assertThat(hp.getProductTitle().startsWith(search));
            assertThat(hp.getOriginalPrice()).isEqualTo(originalPrice);
            assertThat(hp.getHotDealPrice()).isEqualTo(hotDealPrice);
            assertThat(hp.getDiscountRate()).isEqualTo(discountRate);
        });
    }

    @Test
    @DisplayName("hotDealProduct 페이징 조회_성공_search 미 포함")
    public void getHotDealProducts_success_withoutSearch(){
        //given
        int originalPrice = 1000;
        double discountRate = 0.1;
        int stock = 100;
        int hotDealPrice = (int) Math.floor(originalPrice * (1 - discountRate));

        ArrayList<HotDealProduct> expectedHotDealProducts = new ArrayList<>();
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        for(long i = 1; i <= 10; i++){
            HotDealProduct hotDealProduct = createTestHotDealProduct(i, "oddProduct" + i, originalPrice, discountRate, stock);
            ReflectionTestUtils.setField(hotDealProduct, "id", i);
            if (i > 7) expectedHotDealProducts.add(hotDealProduct);
            hotDealProducts.add(hotDealProduct);
        }

        List<HotDealProduct> reversedExpectedHotDealProducts = expectedHotDealProducts.reversed();

        HotDeal hotDeal = createTestHotDeal(hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);

        Long cursor = null;
        String search = null;
        int size = 3;
        Long expectedCursor = 8L;

        when(hotDealProductRepository.findByCursorAndSearchAndSizeHotDealProducts(
                eq(hotDeal.getId()), eq(Long.MAX_VALUE), eq(search), any(Pageable.class)))
                .thenReturn(reversedExpectedHotDealProducts);
        //when
        HotDealProductPagingResponseDto result = hotDealProductService.getHotDealProducts(hotDeal.getId(), search, cursor, size);

        //then
        assertThat(result.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(result.getCursor()).isEqualTo(expectedCursor);
        List<HotDealProductResponseDto> resultHotDealProducts = result.getHotDealProducts();
        assertThat(resultHotDealProducts).hasSize(size);
        resultHotDealProducts.forEach(hp -> {
            assertThat(hp.getOriginalProductId()).isNotNull();
            assertThat(hp.getOriginalPrice()).isEqualTo(originalPrice);
            assertThat(hp.getHotDealPrice()).isEqualTo(hotDealPrice);
            assertThat(hp.getDiscountRate()).isEqualTo(discountRate);
        });
    }

    @Test
    @DisplayName("hotDealProduct 단건 조회_성공")
    public void getHotDealProduct_success(){
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product", 1000, 0.1, 100);
        ReflectionTestUtils.setField(hotDealProduct, "id", 1L);
        createTestHotDeal(List.of(hotDealProduct));

        when(hotDealProductRepository.findById(hotDealProduct.getId())).thenReturn(Optional.of(hotDealProduct));

        //when
        HotDealProductCacheDto result = hotDealProductService.getHotDealProduct(hotDealProduct.getId());

        //then
        assertThat(result.getHotDealProductId()).isEqualTo(hotDealProduct.getId());
        assertThat(result.getOriginalProductId()).isEqualTo(hotDealProduct.getProductId());
        assertThat(result.getProductTitle()).isEqualTo(hotDealProduct.getProductTitle());
        assertThat(result.getOriginalPrice()).isEqualTo(hotDealProduct.getOriginalPrice());
        assertThat(result.getHotDealPrice()).isEqualTo(900);
        assertThat(result.getDiscountRate()).isEqualTo(hotDealProduct.getDiscountRate());
    }

    @Test
    @DisplayName("hotDealProduct 단건 조회_실패")
    public void getHotDealProduct_failure_notFundHotDealProduct(){
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product", 1000, 0.1, 100);
        ReflectionTestUtils.setField(hotDealProduct, "id", 1L);
        createTestHotDeal(List.of(hotDealProduct));

        when(hotDealProductRepository.findById(hotDealProduct.getId())).thenReturn(Optional.empty());

        //when && then
        assertThatThrownBy(() -> hotDealProductService.getHotDealProduct(hotDealProduct.getId())).isInstanceOf(HotDealProductException.class);
    }
}