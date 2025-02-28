package com.hong.hotdealservice;


import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.repository.HotDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class initData {

    private final HotDealRepository hotDealRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init(){
        createHotDeals();
    }

    public void createHotDeals() {
        Random random = new Random();
        int totalProducts = 3000; // 전체 상품 개수
        int totalHotDeals = 300;  // 전체 핫딜 개수
        int totalHotDealProducts = 3000; // 핫딜 상품 개수를 정확히 3000개로 설정

        List<Long> productIds = new ArrayList<>();
        for (long i = 1; i <= totalProducts; i++) {
            productIds.add(i);
        }
        Collections.shuffle(productIds); // 상품 ID를 랜덤하게 섞음

        // 상품 리스트에서 가져올 인덱스
        int productIndex = 0;

        for (long hotDealId = 1; hotDealId <= totalHotDeals; hotDealId++) {
            List<HotDealProduct> hotDealProducts = new ArrayList<>();
            Set<Long> usedProductIds = new HashSet<>();

            int productsPerHotDeal = totalHotDealProducts / totalHotDeals; // 핫딜당 상품 개수 균등 배분
            if (hotDealId <= totalHotDealProducts % totalHotDeals) productsPerHotDeal++; // 나머지 개수 분배

            for (int i = 0; i < productsPerHotDeal; i++) {
                if (productIndex >= totalProducts) break; // 상품 개수를 초과하면 종료

                long productId = productIds.get(productIndex++);
                usedProductIds.add(productId);

                hotDealProducts.add(
                        HotDealProduct.create(
                                productId,
                                "hotDealProduct" + productId,
                                10_000,
                                (random.nextInt(9) + 1) / 10.0, // 0.1 ~ 0.9 랜덤 할인율
                                100000000
                        )
                );
            }

            HotDeal hotDeal = HotDeal.create(
                    hotDealId,
                    "HotDeal " + hotDealId,
                    "Test HotDeal " + hotDealId,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(24),
                    hotDealProducts
            );

            hotDealRepository.save(hotDeal);
        }
    }

}
