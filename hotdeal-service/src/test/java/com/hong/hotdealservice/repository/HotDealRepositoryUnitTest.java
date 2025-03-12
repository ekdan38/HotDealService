package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.config.JpaConfig;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import org.assertj.core.api.Assertions;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
class HotDealRepositoryUnitTest {
    
    @Autowired
    HotDealRepository hotDealRepository;
    @Autowired
    HotDealProductRepository hotDealProductRepository;

    private HotDealProduct createTestHotDealProduct(Long productId, String productTitle,
                                                int originalPrice, double discountRate, int stock){
        return HotDealProduct.create(productId, productTitle, originalPrice, discountRate, stock);
    }

    private HotDeal createTestHotDeal(Long adminId, String title, String description,
                                      LocalDateTime startTime, LocalDateTime endTime,
                                      List<HotDealProduct> hp){
        HotDeal hotDeal = HotDeal.create(adminId, title, description, startTime, endTime, hp);
        hotDealRepository.save(hotDeal);
        return hotDeal;
    }

    @Test
    @DisplayName("hotDeal title 로 조회_존재")
    public void existsByTitle_exists(){
        //given
        String hotDealTitle = "hotDeal";
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product", 1000, 0.2, 1000);
        createTestHotDeal(1L, "hotDeal", "description",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), List.of(hotDealProduct));

        //when
        boolean exists = hotDealRepository.existsByTitle(hotDealTitle);

        //then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("hotDeal title 로 조회_미 존재")
    public void existsByTitle_notExists(){
        //given
        String hotDealTitle = "hotDeal";

        //when
        boolean exists = hotDealRepository.existsByTitle(hotDealTitle);

        //then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("hotDeal 페이징 조회")
    public void findByCursorAndSearchAndSizeHotDeals(){
        //given
        Long maxId = null;
        for(long i = 1; i <= 10; i++){
            HotDealProduct hotDealProduct = createTestHotDealProduct(i, "product" + i, 1000, 0.2, 1000);
            HotDeal hotDeal;
            if(i % 2 == 0) hotDeal = createTestHotDeal(1L, "EvenHotDeal" + i, "description",
                        LocalDateTime.now(), LocalDateTime.now().plusHours(1), List.of(hotDealProduct));
            else hotDeal = createTestHotDeal(1L, "OddHotDeal" + i, "description",
                        LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS), LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusHours(1),
                    List.of(hotDealProduct));
            if(i == 10) maxId = hotDeal.getId();
        }
        Long cursor = maxId + 1L;
        int size = 5;
        String search = "Even";
        PageRequest pageRequest = PageRequest.of(0, size);

        //when
        List<HotDeal> result = hotDealRepository.findByCursorAndSearchAndSizeHotDeals(cursor, search, pageRequest);

        //then
        assertThat(result).hasSize(size);
        for(int i = 1; i < size; i++){
            assertThat(result.get(i - 1).getId()).isGreaterThan(result.get(i).getId());
        }
        for (HotDeal hotDeal : result) {
            assertThat(hotDeal.getTitle().contains("EvenHotDeal")).isTrue();
        }
    }


    @Test
    @DisplayName("hotDealId 로 hotDeal 조회 및 hotDealProducts FETCH JOIN")
    public void findByIdWithHotDealProducts(){
        //given
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime end = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusHours(1);
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product", 10000, 0.3, 100);
        HotDeal hotDeal = createTestHotDeal(1L, "hotDeal", "description",
                start,end, List.of(hotDealProduct));

        //when
        Optional<HotDeal> foundOptionalHotDeal = hotDealRepository.findByIdWithHotDealProducts(hotDeal.getId());

        //then
        assertThat(foundOptionalHotDeal).isPresent();
        HotDeal foundHotDeal = foundOptionalHotDeal.get();
        assertThat(foundHotDeal.getUserId()).isEqualTo(1L);
        assertThat(foundHotDeal.getId()).isEqualTo(hotDeal.getId());
        assertThat(foundHotDeal.getTitle()).isEqualTo(hotDeal.getTitle());
        assertThat(foundHotDeal.getDescription()).isEqualTo(hotDeal.getDescription());
        assertThat(foundHotDeal.getStartTime()).isEqualTo(start);
        assertThat(foundHotDeal.getEndTime()).isEqualTo(end);
        assertThat(foundHotDeal.getHotDealProducts()).hasSize(1);

        HotDealProduct foundHotDealProduct = foundHotDeal.getHotDealProducts().get(0);
        assertThat(foundHotDealProduct.getProductId()).isEqualTo(1L);
        assertThat(foundHotDealProduct.getProductTitle()).isEqualTo("product");
        assertThat(foundHotDealProduct.getOriginalPrice()).isEqualTo(10000);
        assertThat(foundHotDealProduct.getDiscountRate()).isEqualTo(0.3);
        assertThat(foundHotDealProduct.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("hotDealId 로 hotDeal 조회 및 hotDealProducts FETCH JOIN_optional.empty 반환")
    public void findByIdWithHotDealProducts_returnEmpty(){
        //given
        Long hotDealId = 1L;

        //when
        Optional<HotDeal> foundOptionalHotDeal = hotDealRepository.findByIdWithHotDealProducts(hotDealId);

        //then
        assertThat(foundOptionalHotDeal).isNotPresent();
    }

    @Test
    @DisplayName("hotDealIds 로 hotDeal 조회")
    public void findByIds(){
        //given
        ArrayList<Long> hotDealIds = new ArrayList<>();
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime end = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusHours(1);
        for(int i = 1; i <= 3; i++){
            HotDeal hotDeal = createTestHotDeal(1L, "hotDeal" + i, "description", start, end, List.of());
            hotDealIds.add(hotDeal.getId());
        }

        //when
        List<HotDeal> foundHotDeals = hotDealRepository.findByIds(hotDealIds);

        //then
        assertThat(foundHotDeals).hasSize(3);
        for(int i = 1; i <= 3; i++){
            HotDeal hotDeal = foundHotDeals.get(i - 1);
            assertThat(hotDeal.getId()).isNotNull();
            assertThat(hotDeal.getUserId()).isEqualTo(1L);
            assertThat(hotDeal.getTitle()).isEqualTo("hotDeal" + i);
            assertThat(hotDeal.getDescription()).isEqualTo("description");
            assertThat(hotDeal.getStartTime()).isEqualTo(start);
            assertThat(hotDeal.getEndTime()).isEqualTo(end);
            assertThat(hotDeal.getHotDealProducts()).isEmpty();
        }
    }

    @Test
    @DisplayName("hotDeal status == 'SHEDULED' && 이벤트 기간이면 status 'ACTIVE'로 벌크 업데이트_조건 만족")
    void updateScheduledToActive_WithinEventPeriod() {
        // given
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime start = now.minusMinutes(5);
        LocalDateTime end = now.plusMinutes(5);

        ArrayList<Long> hotDealIds = new ArrayList<>();
        for(int i = 1; i <= 3; i++){
            HotDeal hotDeal = createTestHotDeal(1L, "hotDeal" + i, "description", start, end, List.of());
            hotDealIds.add(hotDeal.getId());
            assertThat(hotDeal.getStatus()).isEqualTo(HotDealStatus.SCHEDULED);
        }

        // when
        hotDealRepository.updateScheduledToActive(now);

        // then
        List<HotDeal> foundHotDeals = hotDealRepository.findByIds(hotDealIds);
        foundHotDeals.forEach(h -> assertThat(h.getStatus()).isEqualTo(HotDealStatus.ACTIVE));
    }

    @Test
    @DisplayName("hotDeal status == 'SHEDULED' && 이벤트 기간이면 status 'ACTIVE'로 벌크 업데이트_조건 미 만족")
    void updateScheduledToActive_outSideEventPeriod() {
        // given
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime start = now.plusMinutes(5);
        LocalDateTime end = now.plusMinutes(10);

        ArrayList<Long> hotDealIds = new ArrayList<>();
        for(int i = 1; i <= 3; i++){
            HotDeal hotDeal = createTestHotDeal(1L, "hotDeal" + i, "description", start, end, List.of());
            hotDealIds.add(hotDeal.getId());
            assertThat(hotDeal.getStatus()).isEqualTo(HotDealStatus.SCHEDULED);
        }

        // when
        hotDealRepository.updateScheduledToActive(now);

        // then
        List<HotDeal> foundHotDeals = hotDealRepository.findByIds(hotDealIds);
        foundHotDeals.forEach(h -> assertThat(h.getStatus()).isEqualTo(HotDealStatus.SCHEDULED));
    }

    @Test
    @DisplayName("hotDeal status == 'ACTIVE' && 이벤트 기간 끝났으면 status 'EXPIRED'로 벌크 업데이트_조건 만족")
    void updateScheduledToExpired_withinEventPeriod() {
        // given
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime start = now.minusMinutes(10);
        LocalDateTime end = now.minusMinutes(5);

        ArrayList<Long> hotDealIds = new ArrayList<>();
        for(int i = 1; i <= 3; i++){
            HotDeal hotDeal = createTestHotDeal(1L, "hotDeal" + i, "description", start, end, List.of());
            hotDeal.updateStatus(HotDealStatus.ACTIVE);
            hotDealIds.add(hotDeal.getId());
            assertThat(hotDeal.getStatus()).isEqualTo(HotDealStatus.ACTIVE);
        }

        // when
        hotDealRepository.updateScheduledToExpired(now);

        // then
        List<HotDeal> foundHotDeals = hotDealRepository.findByIds(hotDealIds);
        foundHotDeals.forEach(h -> assertThat(h.getStatus()).isEqualTo(HotDealStatus.EXPIRED));
    }

    @Test
    @DisplayName("hotDeal status == 'ACTIVE' && 이벤트 기간 끝났으면 status 'EXPIRED'로 벌크 업데이트_조건 미 만족")
    void updateScheduledToExpired_outSideEventPeriod() {
        // given
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime start = now.minusMinutes(10);
        LocalDateTime end = now.plusMinutes(10);

        ArrayList<Long> hotDealIds = new ArrayList<>();
        for(int i = 1; i <= 3; i++){
            HotDeal hotDeal = createTestHotDeal(1L, "hotDeal" + i, "description", start, end, List.of());
            hotDeal.updateStatus(HotDealStatus.ACTIVE);
            hotDealIds.add(hotDeal.getId());
            assertThat(hotDeal.getStatus()).isEqualTo(HotDealStatus.ACTIVE);
        }

        // when
        hotDealRepository.updateScheduledToExpired(now);

        // then
        List<HotDeal> foundHotDeals = hotDealRepository.findByIds(hotDealIds);
        foundHotDeals.forEach(h -> {
            assertThat(h.getStatus()).isEqualTo(HotDealStatus.ACTIVE);
            assertThat(h.getExpiredAt()).isNull();
        });
    }

    @Test
    @DisplayName("expired 된 hotDeal 조회 및 hotDealProducts FETCH JOIN")
    public void findHotDealsByExpiredAtNow(){
        //given
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime start = now.minusMinutes(10);
        LocalDateTime end = now.minusMinutes(5);

        for(int i = 1; i <= 3; i++){
            HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product", 10000, 0.3, 100);
            HotDeal hotDeal = createTestHotDeal(1L, "hotDeal" + i, "description",
                    start,end, List.of(hotDealProduct));
            hotDeal.updateStatus(HotDealStatus.ACTIVE);
        }
        hotDealRepository.updateScheduledToExpired(now);

        //when
        List<HotDeal> foundHotDeals = hotDealRepository.findHotDealsByExpiredAtNow(now);

        //then
        assertThat(foundHotDeals).hasSize(3);
        for(int i = 1; i <= 3; i++){
            HotDeal foundHotDeal = foundHotDeals.get(i - 1);
            assertThat(foundHotDeal.getUserId()).isEqualTo(1L);
            assertThat(foundHotDeal.getId()).isNotNull();
            assertThat(foundHotDeal.getTitle()).isEqualTo("hotDeal" + i);
            assertThat(foundHotDeal.getDescription()).isEqualTo("description");
            assertThat(foundHotDeal.getStartTime()).isEqualTo(start);
            assertThat(foundHotDeal.getEndTime()).isEqualTo(end);
            assertThat(foundHotDeal.getHotDealProducts()).hasSize(1);
            HotDealProduct foundHotDealProduct = foundHotDeal.getHotDealProducts().get(0);
            assertThat(foundHotDealProduct.getProductId()).isEqualTo(1L);
            assertThat(foundHotDealProduct.getProductTitle()).isEqualTo("product");
            assertThat(foundHotDealProduct.getOriginalPrice()).isEqualTo(10000);
            assertThat(foundHotDealProduct.getDiscountRate()).isEqualTo(0.3);
            assertThat(foundHotDealProduct.getStock()).isEqualTo(100);
        }
    }
}