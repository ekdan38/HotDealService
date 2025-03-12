package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.config.JpaConfig;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.HotDealProductStockProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class HotDealProductRepositoryUnitTest {

    @Autowired
    HotDealProductRepository hotDealProductRepository;
    @Autowired
    HotDealRepository hotDealRepository;

    private HotDealProduct createTestHotDealProduct(Long productId, String productTitle,
                                                    int originalPrice, double discountRate, int stock){
        return HotDealProduct.create(productId, productTitle, originalPrice, discountRate, stock);
    }

    private HotDeal createTestHotDeal(List<HotDealProduct> hp){
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime start = now.minusMinutes(5);
        LocalDateTime end = now.plusMinutes(5);
        HotDeal hotDeal = HotDeal.create(1L, "hotDeal", "description", start, end, hp);
        hotDealRepository.save(hotDeal);
        return hotDeal;
    }

    @Test
    @DisplayName("hotDealProductIds 로 hotDealProducts 조회")
    public void findByIds(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        for(int i = 1; i <= 3; i++){
            hotDealProducts.add(createTestHotDealProduct((long) i, "product" + i, 1000, 0.2, 100));
        }
        createTestHotDeal(hotDealProducts);
        List<Long> hotDealProductIds = hotDealProducts
                .stream()
                .map(HotDealProduct::getId)
                .toList();

        //when
        List<HotDealProduct> foundHotDealProducts = hotDealProductRepository.findByIds(hotDealProductIds);

        //then
        assertThat(foundHotDealProducts).hasSize(3);
        foundHotDealProducts.forEach(hp -> {
            assertThat(hp.getId()).isNotNull();
            assertThat(hp.getProductId()).isNotNull();
            assertThat(hp.getOriginalPrice()).isEqualTo(1000);
            assertThat(hp.getDiscountRate()).isEqualTo(0.2);
            assertThat(hp.getStock()).isEqualTo(100);
        });
    }

    @Test
    @DisplayName("hotDealProductIds 로 stock DtoProjection 조회")
    public void findStockByProductIds() {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            hotDealProducts.add(createTestHotDealProduct((long) i, "product" + i, 1000, 0.2, 1000 * i));
        }
        createTestHotDeal(hotDealProducts);
        List<Long> hotDealProductIds = hotDealProducts
                .stream()
                .map(HotDealProduct::getId)
                .toList();

        //when
        List<HotDealProductStockProjection> stockDtos = hotDealProductRepository.findStockByProductIds(hotDealProductIds);

        //then
        assertThat(stockDtos).hasSize(3);
        for (int i = 1; i <= 3; i++) {
            HotDealProductStockProjection stockDto = stockDtos.get(i - 1);
            assertThat(stockDto.getGetId()).isNotNull();
            assertThat(stockDto.getGetStock()).isEqualTo(1000 * i);
        }
    }

    @Test
    @DisplayName("hotDealProducts 페이징 조회_search 포함")
    public void findByCursorAndSearchAndSizeHotDealProducts_wistSearch() {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            if(i % 2 == 0) hotDealProducts.add(createTestHotDealProduct((long) i, "evenProduct" + i,
                    1000, 0.1, 100 * i));
            else hotDealProducts.add(createTestHotDealProduct((long) i, "oddProduct" + i,
                    2000, 0.2, 200 * i));
        }
        HotDeal hotDeal = createTestHotDeal(hotDealProducts);
        Long cursor = hotDeal.getHotDealProducts().get(hotDeal.getHotDealProducts().size() - 1).getId() + 1;
        int size = 5;
        String search = "even";
        PageRequest pageRequest = PageRequest.of(0, size);

        //when
        List<HotDealProduct> result = hotDealProductRepository.findByCursorAndSearchAndSizeHotDealProducts(hotDeal.getId(), cursor, search, pageRequest);

        //then
        assertThat(result).hasSize(size);
        // 정렬 확인
        for(int i = 1; i < size; i++){
            assertThat(result.get(i - 1).getId()).isGreaterThan(result.get(i).getId());
        }
        // Search 조건 확인
        for (HotDealProduct hp : result) {
            assertThat(hp.getProductTitle().contains("evenProduct")).isTrue();
        }
    }

    @Test
    @DisplayName("hotDealProducts 페이징 조회_search 미 포함")
    public void findByCursorAndSearchAndSizeHotDealProducts_wistOutSearch() {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            if(i % 2 == 0) hotDealProducts.add(createTestHotDealProduct((long) i, "evenProduct" + i,
                    1000, 0.1, 100 * i));
            else hotDealProducts.add(createTestHotDealProduct((long) i, "oddProduct" + i,
                    2000, 0.2, 200 * i));
        }
        HotDeal hotDeal = createTestHotDeal(hotDealProducts);
        Long cursor = hotDeal.getHotDealProducts().get(hotDeal.getHotDealProducts().size() - 1).getId() + 1;
        int size = 5;
        PageRequest pageRequest = PageRequest.of(0, size);

        //when
        List<HotDealProduct> result = hotDealProductRepository.findByCursorAndSearchAndSizeHotDealProducts(hotDeal.getId(), cursor, null, pageRequest);

        //then
        assertThat(result).hasSize(size);
        // 정렬 확인
        for(int i = 1; i < size; i++){
            assertThat(result.get(i - 1).getId()).isGreaterThan(result.get(i).getId());
        }
    }
}