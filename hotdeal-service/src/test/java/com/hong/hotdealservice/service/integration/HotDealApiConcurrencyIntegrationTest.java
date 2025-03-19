package com.hong.hotdealservice.service.integration;

import com.hong.common.dto.HotDealProductStockUpdateRequestDto;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import com.hong.hotdealservice.repository.HotDealProductRepository;
import com.hong.hotdealservice.repository.HotDealRepository;
import com.hong.hotdealservice.service.HotDealApiService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HotDealApiConcurrencyIntegrationTest {

    @Autowired
    HotDealApiService hotDealApiService;
    @Autowired
    HotDealRepository hotDealRepository;
    @Autowired
    HotDealProductRepository hotDealProductRepository;

    private List<HotDeal> hotDeals = new ArrayList<>();

    @BeforeEach
    void setUp(){
        HotDeal hotDeal1 = HotDeal.create(1L, "HotDeal1", "hotDeal1",
                LocalDateTime.now(), LocalDateTime.now().plusHours(24), List.of(
                        HotDealProduct.create(1L, "product1", 10000, 0.1, 100),
                        HotDealProduct.create(2L, "product2", 20000, 0.2, 100)
                ));
        hotDeal1.updateStatus(HotDealStatus.ACTIVE);
        hotDealRepository.save(hotDeal1);
        hotDeals.add(hotDeal1);

        HotDeal hotDeal2 = HotDeal.create(1L, "HotDeal2", "hotDeal2",
                LocalDateTime.now(), LocalDateTime.now().plusHours(24), List.of(
                        HotDealProduct.create(3L, "product1", 10000, 0.1, 100),
                        HotDealProduct.create(4L, "product2", 20000, 0.2, 100)
                ));
        hotDeal2.updateStatus(HotDealStatus.ACTIVE);
        hotDealRepository.save(hotDeal2);
        hotDeals.add(hotDeal2);
    }

    @AfterEach
    void after(){
        hotDealProductRepository.deleteAll();
        hotDealRepository.deleteAll();
    }

    @Test
    @DisplayName("hotDealProduct 재고 감소_동시성 테스트_성공")
    public void decreaseStock_success() throws InterruptedException {
        //given
        List<HotDealProductStockUpdateRequestDto> hotDealProductStockUpdateRequestDto = new ArrayList<>();
        for (HotDeal hotDeal : hotDeals) {
            for (HotDealProduct hotDealProduct : hotDeal.getHotDealProducts()) {
                hotDealProductStockUpdateRequestDto.add(new HotDealProductStockUpdateRequestDto(hotDealProduct.getId(), 1));
            }
        }

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        
        //when
        for(int i = 0; i < numberOfThreads; i++){
            executorService.submit(() -> {
                try{
                    hotDealApiService.decreaseStock(hotDealProductStockUpdateRequestDto);
                }
                finally {
                    latch.countDown();
                }
            });
        }
        latch.await();


        //then
        for (HotDeal hotDeal : hotDeals) {
            for (HotDealProduct hotDealProduct : hotDeal.getHotDealProducts()) {
                HotDealProduct foundHotDealProduct = hotDealProductRepository.findById(hotDealProduct.getId()).orElseThrow();
                Assertions.assertThat(foundHotDealProduct.getStock()).isEqualTo(0);
            }
        }
    }

    @Test
    @DisplayName("hotDealProduct 재고 증가_동시성 테스트_성공")
    public void increaseStock_Success() throws InterruptedException {
        //given
        List<HotDealProductStockUpdateRequestDto> hotDealProductStockUpdateRequestDtos = new ArrayList<>();
        for (HotDeal hotDeal : hotDeals) {
            for (HotDealProduct hotDealProduct : hotDeal.getHotDealProducts()) {
                hotDealProductStockUpdateRequestDtos.add(new HotDealProductStockUpdateRequestDto(hotDealProduct.getId(), 1));
            }
        }

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);


        //when
        for(int i = 0; i < numberOfThreads; i++){
            executorService.submit(() -> {
                try{
                    hotDealApiService.increaseStock(hotDealProductStockUpdateRequestDtos);
                }
                finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        //then
        for (HotDeal hotDeal : hotDeals) {
            for (HotDealProduct hotDealProduct : hotDeal.getHotDealProducts()) {
                HotDealProduct foundHotDealProduct = hotDealProductRepository.findById(hotDealProduct.getId()).orElseThrow();
                Assertions.assertThat(foundHotDealProduct.getStock()).isEqualTo(200);

            }
        }
    }


}