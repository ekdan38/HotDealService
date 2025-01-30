package com.hong.hotdealservice.service;

import com.hong.common.dto.HotDealProductCommonDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealException;
import com.hong.common.exception.custom.HotDealProductException;
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

    // 핫딜 상품 재고 감소
    @Transactional
    public List<HotDealProductCommonDto> fetchAndDecreaseStock(List<HotDealProductCommonDto> hotDealProductCommonDtos) {
        // response List
        List<HotDealProductCommonDto> responseDtos;
        // hotDealProductIds 추출
        List<Long> hotDealProductIds = extractHotDealProductIds(hotDealProductCommonDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(hotDealProductIds);
        List<Long> hotDealIds = extractHotDealIds(hotDealProductCommonDtos);
        // hotDeal 조회, 검증
        List<HotDeal> hotDeals = fetchHotDealWithProductsAndValidate(hotDealIds);
        // <hotDealId, <HotDealProductDto> 형태 Map 변환
        Map<Long, List<HotDealProductCommonDto>> hotDealProductMap = buildHotDealProductMap(hotDealProductCommonDtos);
        // hotDealIds 추출
        // hotDealProducts 요청 검증, 재고 감소
        responseDtos = dereaseStockAndValidateRequestedHotDealProducts(hotDeals, hotDealProductMap);
        // 락 해제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                releaseLocks(locks);
            }
        });
        return responseDtos;
    }

    // 핫딜 상품 재고 증가
    @Transactional
    public List<HotDealProductCommonDto> fetchAndIncreaseStock(List<HotDealProductCommonDto> hotDealProductCommonDtos) {
        // response List
        List<HotDealProductCommonDto> responseDtos;
        // hotDealProductIds 추출
        List<Long> hotDealProductIds = extractHotDealProductIds(hotDealProductCommonDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(hotDealProductIds);
        // <hotDealId, <HotDealProductDto> 형태 Map 변환
        Map<Long, List<HotDealProductCommonDto>> hotDealProductMap = buildHotDealProductMap(hotDealProductCommonDtos);
        // hotDealIds 추출
        List<Long> hotDealIds = extractHotDealIds(hotDealProductCommonDtos);
        // hotDeal 조회, 검증
        List<HotDeal> hotDeals = fetchHotDealWithProductsAndValidate(hotDealIds);
        // hotDealProducts 요청 검증, 재고 증가
        responseDtos = increaseStockAndValidateRequestedHotDealProducts(hotDeals, hotDealProductMap);
        // 락 해제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                releaseLocks(locks);
            }
        });

        return responseDtos;
    }


    // hotDealProductIds 추출
    private List<Long> extractHotDealProductIds(List<HotDealProductCommonDto> hotDealProductCommonDtos) {
        List<Long> hotDealProductIds = hotDealProductCommonDtos.stream()
                .map(HotDealProductCommonDto::getHotDealProductId)
                .sorted()
                .collect(Collectors.toList());
        return hotDealProductIds;
    }

    // 락 획득
    private List<RLock> acquireLocks(List<Long> hotDealProductIds) {
        // 락 객체 목록 생성
        List<RLock> locks = new ArrayList<>();
        // 핫딜 상품 락 생성
        for (Long hotDealProductId : hotDealProductIds) {
            String lockKey = "hot_deal_product_lock:" + hotDealProductId;
            log.info("락 획득 시도 key = {}", lockKey);
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean isLocked = lock.tryLock(10L, 10L, TimeUnit.SECONDS);
                if (!isLocked) {
                    log.debug("hotDealProduct = {} 에 대한 락 획득에 실패했습니다.", hotDealProductId);
                    throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_LOCK_FAILED, hotDealProductId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("hotDealProduct = {} 에 대한 락 획득중 입터럽트가 발생했습니다.", hotDealProductId);
                throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_LOCK_INTERRUPTED, hotDealProductId);
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
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("락 해제 key = {}", lockKey);
            }
        }
    }

    // hotDeal 재고 감소, hotDealProducts 요청 검증
    private List<HotDealProductCommonDto> dereaseStockAndValidateRequestedHotDealProducts(List<HotDeal> hotDeals, Map<Long, List<HotDealProductCommonDto>> hotDealProductMap) {
        List<HotDealProductCommonDto> hotDealProductCommonDtos = new ArrayList<>();
        for (HotDeal hotDeal : hotDeals) {
            // hotDealId로 map에서 추출
            // hotDealPorudctId 랑 dto의 productId랑 일치 하는 hotDelProduct 추출
            List<HotDealProductCommonDto> dtos = hotDealProductMap.get(hotDeal.getId());
            List<HotDealProduct> hotDealProducts = hotDeal.getHotDealProducts().stream()
                    .filter(hp -> dtos.stream()
                            .anyMatch(dto -> dto.getHotDealProductId().equals(hp.getId())))
                    .collect(Collectors.toList());

            // hotDealPorudctId 랑 dto의 productId랑 일치 하지 않는 hotDelProduct 추출 => 디버깅, 에러 처리 용도
            List<Long> noneMathHotDealProductIds = hotDeal.getHotDealProducts().stream()
                    .filter(hp -> dtos.stream()
                            .noneMatch(dto -> dto.getHotDealProductId().equals(hp.getId())))
                    .map(HotDealProduct::getId)
                    .collect(Collectors.toList());

            if (hotDealProducts.size() != dtos.size()) {
                log.debug("요청된 핫딜 상품이 존재하지 않습니다. = {}", noneMathHotDealProductIds);
                throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, noneMathHotDealProductIds);
            }

            for (HotDealProduct hotDealProduct : hotDealProducts) {
                Integer quantity = dtos.stream()
                        .filter(dto -> dto.getHotDealProductId().equals(hotDealProduct.getId()))
                        .findFirst()
                        .map(HotDealProductCommonDto::getQuantity)
                        .orElseThrow(() -> {
                            log.debug("요청된 핫딜 상품에 대한 수량이 누락 되었습니다. hotDealProductId = {}", hotDealProduct.getId());
                            return new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_INVALID_FOUND, hotDealProduct.getProductId());
                        });
                log.info("hotDealProductId = {}, 조회 재고 = {}", hotDealProduct.getProductId(), hotDealProduct.getStock());
                // 재고 감소
                hotDealProduct.decreaseStock(quantity);
                log.info("hotDealProductId = {}, 재고 감소 = {}", hotDealProduct.getProductId(), hotDealProduct.getStock());
                // hotDealProduct List 반환
                hotDealProductCommonDtos.add(new HotDealProductCommonDto(
                        hotDeal.getId(),
                        hotDealProduct.getId(),
                        hotDealProduct.getProductId(),
                        hotDealProduct.getProductTitle(),
                        quantity, hotDealProduct.getHotDealPrice()));
            }
        }
        return hotDealProductCommonDtos;
    }

    // hotDeal 재고 증가, hotDealProducts 요청 검증
    private List<HotDealProductCommonDto> increaseStockAndValidateRequestedHotDealProducts(List<HotDeal> hotDeals, Map<Long, List<HotDealProductCommonDto>> hotDealProductMap) {
        List<HotDealProductCommonDto> hotDealProductCommonDtos = new ArrayList<>();
        for (HotDeal hotDeal : hotDeals) {
            // hotDealId로 map에서 추출
            // hotDealPorudctId 랑 dto의 productId랑 일치 하는 hotDelProduct 추출
            List<HotDealProductCommonDto> dtos = hotDealProductMap.get(hotDeal.getId());
            List<HotDealProduct> hotDealProducts = hotDeal.getHotDealProducts().stream().filter(hp -> dtos.stream()
                            .anyMatch(dto -> dto.getHotDealProductId().equals(hp.getId())))
                    .collect(Collectors.toList());

            // hotDealPorudctId 랑 dto의 productId랑 일치 하지 않는 hotDelProduct 추출 => 디버깅, 에러 처리 용도
            List<Long> noneMathHotDealProductIds = hotDeal.getHotDealProducts().stream()
                    .filter(hp -> dtos.stream()
                            .noneMatch(dto -> dto.getHotDealProductId().equals(hp.getId())))
                    .map(HotDealProduct::getId)
                    .collect(Collectors.toList());

            if (hotDealProducts.size() != dtos.size()) {
                log.debug("요청된 핫딜 상품이 존재하지 않습니다. = {}", noneMathHotDealProductIds);
                throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, noneMathHotDealProductIds);
            }

            for (HotDealProduct hotDealProduct : hotDealProducts) {
                Integer quantity = dtos.stream().filter(dto -> dto.getHotDealProductId().equals(hotDealProduct.getId()))
                        .findFirst()
                        .map(HotDealProductCommonDto::getQuantity)
                        .orElseThrow(() -> {
                            log.debug("요청된 핫딜 상품에 대한 수량이 누락 되었습니다. hotDealProductId = {}", hotDealProduct.getId());
                            return new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_INVALID_FOUND, hotDealProduct.getProductId());
                        });
                log.info("hotDealProductId = {}, 조회 재고 = {}", hotDealProduct.getProductId(), hotDealProduct.getStock());
                // 재고 증가
                hotDealProduct.increaseStock(quantity);
                log.info("hotDealProductId = {}, 재고 감소 = {}", hotDealProduct.getProductId(), hotDealProduct.getStock());
                // hotDealProduct List 반환
                hotDealProductCommonDtos.add(new HotDealProductCommonDto(
                        hotDeal.getId(),
                        hotDealProduct.getId(),
                        hotDealProduct.getProductId(),
                        hotDealProduct.getProductTitle(),
                        quantity,
                        hotDealProduct.getHotDealPrice()));
            }
        }
        return hotDealProductCommonDtos;
    }

    // hotDealIds 추출
    private List<Long> extractHotDealIds(List<HotDealProductCommonDto> hotDealProductCommonDtos) {
        return hotDealProductCommonDtos.stream()
                .map(HotDealProductCommonDto::getHotDealId)
                .distinct()
                .collect(Collectors.toList());
    }

    // hotDeal 조회, 검증
    private List<HotDeal> fetchHotDealWithProductsAndValidate(List<Long> hotDealIds) {
        List<HotDeal> hotDeals = hotDealRepository.findByIdsWithHotDealProducts(hotDealIds);

        List<Long> nonMatchHotDealIds = hotDealIds.stream()
                .filter(id -> hotDeals.stream()
                        .noneMatch(hotDeal -> hotDeal.getId().equals(id)))
                .collect(Collectors.toList());

        // 조회된 값 여부 확인
        if (hotDeals.size() != hotDealIds.size()) {
            log.debug("요청된 핫딜이 존재하지 않습니다. hotDealProductId = {}", nonMatchHotDealIds);
            throw new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND, nonMatchHotDealIds);
        }
        return hotDeals;
    }

    // <hotDealId, <HotDealProductDto> 형태 Map 변환
    private Map<Long, List<HotDealProductCommonDto>> buildHotDealProductMap(List<HotDealProductCommonDto> hotDealProductCommonDtos) {
        return hotDealProductCommonDtos.stream()
                .collect(Collectors.groupingBy(HotDealProductCommonDto::getHotDealId));
    }

}
