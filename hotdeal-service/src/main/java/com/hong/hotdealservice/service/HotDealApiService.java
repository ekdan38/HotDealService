package com.hong.hotdealservice.service;

import com.hong.common.dto.HotDealProductDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealException;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.repository.HotDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealApiService]")
@Transactional(readOnly = true)
public class HotDealApiService {

    private final HotDealRepository hotDealRepository;
    private final RedissonClient redissonClient;

    @Transactional
    public Boolean fetchAndDecreaseStock(List<HotDealProductDto> hotDealProductDtos){
        // hotDealProductIds 추출
        List<Long> hotDealProductIds = extractHotDealProductIds(hotDealProductDtos);
        try{
            // 락 획득
            List<RLock> locks = acquireLocks(hotDealProductIds);
            // <hotDealId, <HotDealProductDto> 형태 Map 변환
            Map<Long, List<HotDealProductDto>> hotDealProductMap = buildHotDealProductMap(hotDealProductDtos);
            // hotDealIds 추출
            List<Long> hotDealIds = extractHotDealIds(hotDealProductDtos);
            // hotDeal 조회, 검증
            List<HotDeal> hotDeals = fetchHotDealWithProductsAndValidate(hotDealIds);
            // hotDealProducts 요청 검증, 재고 감소
            dereaseStockAndValidateRequestedHotDealProducts(hotDeals, hotDealProductMap);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    releaseLocks(locks);
                }
            });

        }
        catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new HotDealException(ErrorCode.HOTDEAL_LOCK_FAILED);
        }
        return true;
    }

    // hotDealProductIds 추출
    private List<Long> extractHotDealProductIds(List<HotDealProductDto> hotDealProductDtos) {
        List<Long> hotDealProductIds = hotDealProductDtos.stream().map(HotDealProductDto::getHotDealProductId)
                .collect(Collectors.toList());
        return hotDealProductIds;
    }

    // 락 획득
    private List<RLock> acquireLocks(List<Long> hotDealProductIds) throws InterruptedException {
        // 락 객체 목록 생성
        List<RLock> locks = new ArrayList<>();
        // 락 키 정렬로 데드락 방지
        List<Long> sortedIds = hotDealProductIds.stream().sorted().collect(Collectors.toList());
        // 핫딜 상품 락 생성
        for (Long hotDealProductId : sortedIds) {
            String lockKey = "hot_deal_product_lock:" + hotDealProductId;
            log.info("락 획득 시도 key = {}", lockKey);
            RLock lock = redissonClient.getLock(lockKey);
            boolean isLocked = lock.tryLock(10L, 10L, TimeUnit.SECONDS);
            if(!isLocked) {
                log.error("락 획득 실패 key = {} ", lockKey);
                throw new HotDealException(ErrorCode.HOTDEAL_LOCK_FAILED);
            }
            locks.add(lock);
            log.info("락 획득 성공 key = {}", lockKey);
        }
        return locks;
    }
    // 락 해제
    private void releaseLocks(List<RLock> locks) {
        for (RLock lock : locks) {
            String lockKey = lock.getName();
            if(lock != null && lock.isHeldByCurrentThread()){
                lock.unlock();
                log.info("락 해제 key = {}", lockKey);
            }
        }
    }

    // hotDeal 재고 감소, hotDealProducts 요청 검증
    private void dereaseStockAndValidateRequestedHotDealProducts(List<HotDeal> hotDeals, Map<Long, List<HotDealProductDto>> hotDealProductMap) {
        for (HotDeal hotDeal : hotDeals) {
            // hotDealId로 map에서 추출
            // hotDealPorudctId 랑  dto의 productId랑 같은거 추출
            List<HotDealProductDto> dtos = hotDealProductMap.get(hotDeal.getId());
            List<HotDealProduct> hotDealProducts = hotDeal.getHotDealProducts().stream().filter(hp -> dtos.stream()
                            .anyMatch(dto -> dto.getHotDealProductId().equals(hp.getId())))
                    .collect(Collectors.toList());

            if(hotDealProducts.size() != dtos.size()){
                log.error("주문 요청된 핫딜 상품중에 잘못된 상품이 존재합니다.");
                throw new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND_PRODUCT);
            }

            for (HotDealProduct hotDealProduct : hotDealProducts) {
                Integer quantity = dtos.stream().filter(dto -> dto.getHotDealProductId().equals(hotDealProduct.getId())).findFirst()
                        .map(HotDealProductDto::getQuantity)
                        .orElseThrow(() -> new IllegalStateException("수량 없음"));
                log.info("hotDealProductId = {}, 조회 재고 = {}", hotDealProduct.getProductId(), hotDealProduct.getStock());
                // 재고 감소
                hotDealProduct.decreaseStock(quantity);
                log.info("hotDealProductId = {}, 재고 감소 = {}", hotDealProduct.getProductId(), hotDealProduct.getStock());
            }
        }
    }

    // hotDealIds 추출
    private List<Long> extractHotDealIds(List<HotDealProductDto> hotDealProductDtos) {
        return hotDealProductDtos.stream()
                .map(HotDealProductDto::getHotDealId)
                .collect(Collectors.toList());
    }

    // hotDeal 조회, 검증
    private List<HotDeal> fetchHotDealWithProductsAndValidate(List<Long> hotDealIds) {
        List<HotDeal> hotDeals = hotDealRepository.findByIdsWithHotDealProducts(hotDealIds);
        // 조회된 값 여부 확인
        if(hotDeals.isEmpty()){
            log.error("조회된 핫딜이 없습니다. hotDealIds = {}", hotDealIds);
            throw new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND);
        }
        // 활성화 된 핫딜인지 확인
        for (HotDeal hotDeal : hotDeals) {
            if(!hotDeal.isActive()){
                log.error("활성화 된 핫딜이 아닙니다.  hotDealIds = {}", hotDealIds);
                throw new HotDealException(ErrorCode.HOTDEAL_IS_NOT_ACTIVE);
            }
        }
        return hotDeals;
    }

    // <hotDealId, <HotDealProductDto> 형태 Map 변환
    private Map<Long, List<HotDealProductDto>> buildHotDealProductMap(List<HotDealProductDto> hotDealProductDtos) {
         return hotDealProductDtos.stream()
                .collect(Collectors.groupingBy(HotDealProductDto::getHotDealId));
    }

}
