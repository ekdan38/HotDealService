package com.hong.hotdealservice.service;

import com.hong.common.dto.*;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealException;
import com.hong.common.exception.custom.HotDealProductException;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.HotDealProductStockDto;
import com.hong.hotdealservice.repository.HotDealProductRepository;
import com.hong.hotdealservice.repository.HotDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealApiService]")
@Transactional(readOnly = true)
public class HotDealApiService {

    private final HotDealProductRepository hotDealProductRepository;
    private final HotDealRepository hotDealRepository;
    private final RedissonClient redissonClient;


    // 단순 핫딜 상품 재고 조회
    // 추후 캐싱 예정
    public List<HotDealProductStockDto> getHotDealProductStocks(List<Long> hotDealProductIds) {
        // hotDealProducts 조회, hotDealProduct 존재 검증
        // 추후 캐싱
        List<HotDealProduct> foundHotDealProducts = fetchHotDealProductsAndValidate(hotDealProductIds);

        return convertToHotDealProductStockDto(foundHotDealProducts);
    }


    // 핫딜 상품 조회, 재고 확인
    public List<HotDealProductStockCheckResponseDto> fetchHotDealProductStockAndValidateStocks(List<HotDealProductStockCheckRequestDto> requestDtos) {
        // 요청 Dto 에서 hotDealId 추출
        List<Long> hotDealIds = extractHotDealIdsFromCheckDto(requestDtos);

        // hotDeal 조회, 존재 검증, 주문 가능 검증
        fetchHotDealAndValidateHotDealAndOrderable(hotDealIds);

        // HotDealProductStockCheckRequestDto 에서 hotDealProductIds 추출
        List<Long> hotDealProductIds = extractHotDealProductIdsFromStockCheckRequestDto(requestDtos);

        // hotDealProduct stock 조회, hotDealProduct 존재 검증
        List<HotDealProductStockDto> stockDtos = getHotDealProductStocks(hotDealProductIds);

        // hotDealProducts 요청 검증 Dto 변환
        return validateRequestedHotDealProductsAndConvertDto(stockDtos, requestDtos);
    }

    // 핫딜 상품 재고 감소
    @Transactional
    public List<HotDealProductStockUpdateResponseDto> decreaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos) {
        // 요청 dto 에서 hotDealProductIds 추출
        List<Long> hotDealProductIds = extractHotDealProductIdsFromUpdateDto(requestDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(hotDealProductIds);

        // HotDealProductStockUpdateRequestDto 에서 hotDealIds 추출
        List<Long> hotDealIds = extractHotDealIdsFromUpdateDto(requestDtos);

        // hotDeal 조회, 존재 검증, 주문 가능 검증
        fetchHotDealAndValidateHotDealAndOrderable(hotDealIds);

        // hotDealProduct 재고 감소, 재고 감소 가능 검증
        List<HotDealProductStockUpdateResponseDto> responseDtos = decreaseStockAndValidateAndConvertResponseDto(hotDealProductIds, requestDtos);
        // 트랜잭션 끝나기 전에 락 해제
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
    public List<HotDealProductStockUpdateResponseDto> increaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos) {
        // 요청 dto 에서 hotDealProductIds 추출
        List<Long> hotDealProductIds = extractHotDealProductIdsFromUpdateDto(requestDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(hotDealProductIds);

        // HotDealProductStockUpdateRequestDto 에서 hotDealIds 추출
        List<Long> hotDealIds = extractHotDealIdsFromUpdateDto(requestDtos);

        // hotDeal 조회, 존재 검증
        fetchHotDealWithProductsAndValidateForIncrease(hotDealIds);

        // hotDealProduct 재고 증가, 재고 증가 가능 검증
        List<HotDealProductStockUpdateResponseDto> responseDtos = increaseStockAndValidateAndConvertResponseDto(hotDealProductIds, requestDtos);
        // 트랜잭션 끝나기 전에 락 해제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                releaseLocks(locks);
            }
        });
        return responseDtos;
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
                    log.warn("hotDealProduct = {} 에 대한 락 획득에 실패했습니다.", hotDealProductId);
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

    // HotDealProductStockCheckRequestDto 에서 hotDealId 추출
    private List<Long> extractHotDealIdsFromCheckDto(List<HotDealProductStockCheckRequestDto> requestDtos) {
        return requestDtos.stream()
                .map(HotDealProductStockCheckRequestDto::getHotDealId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // hotDealProductIds 추출
    private List<Long> extractHotDealProductIdsFromUpdateDto(List<HotDealProductStockUpdateRequestDto> requestDtos) {
        return requestDtos.stream()
                .map(HotDealProductStockUpdateRequestDto::getHotDealProductId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // hotDealProducts 조회, hotDealProduct 존재 검증
    // 추후 캐싱
    private List<HotDealProduct> fetchHotDealProductsAndValidate(List<Long> hotDealProductIds) {
        // hotDealProducts 조회
        List<HotDealProduct> foundHotDealProducts = hotDealProductRepository.findByIds(hotDealProductIds);

        // 조회된 hotDealProductIds 추출
        Set<Long> foundHotDealProductIds = foundHotDealProducts.stream()
                .map(HotDealProduct::getId)
                .collect(Collectors.toSet());

        // reqeust Ids 중에서 조회되지 않은 ids 추출
        List<Long> noneMatchedHotDealProductIds = hotDealProductIds.stream()
                .filter(id -> !foundHotDealProductIds.contains(id))
                .collect(Collectors.toList());

        // 존재 하지 않는 hotDealProduct 예외
        if (!noneMatchedHotDealProductIds.isEmpty()) {
            log.debug("요청된 핫딜 상품이 존재하지 않습니다. hotDealProductId = {}", noneMatchedHotDealProductIds);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, noneMatchedHotDealProductIds);
        }
        return foundHotDealProducts;
    }
    // hotDealProducts 요청 검증 Dto 변환
    private List<HotDealProductStockCheckResponseDto> validateRequestedHotDealProductsAndConvertDto(List<HotDealProductStockDto> stockDtos,
                                                                                                    List<HotDealProductStockCheckRequestDto> requestDtos) {
        // return 에 사용할 list
        List<HotDealProductStockCheckResponseDto> responseDtos = new ArrayList<>();

        // 요청 수량보다 재고가 부족한 요청 모아둘 list
        List<Long> insufficientStockIds = new ArrayList<>();

        // 재고 확인
        for (HotDealProductStockCheckRequestDto request : requestDtos) {
            HotDealProductStockDto stockDto = stockDtos.stream()
                    .filter(dto -> dto.getHotDealProductId().equals(request.getHotDealProductId()))
                    .findFirst()
                    .get(); // 상위 메서드에서 hotDealProduct 에 대한 검증을 마치기에 get() 사용

            // 요청 수량보다 재고가 부족하면 insufficientStockIds 에 add
            if(request.getQuantity() > stockDto.getStock()){
                log.debug("요청 수량보다 재고가 부족합니다. hotDealProductId = {}, stock = {}, requestQuantity = {}"
                        , request.getHotDealProductId(), stockDto.getStock(), request.getQuantity());
                insufficientStockIds.add(request.getHotDealProductId());
            }

            // return list에 add
            responseDtos.add(new HotDealProductStockCheckResponseDto(
                    request.getHotDealId(),
                    stockDto.getHotDealProductId(),
                    stockDto.getProductId(),
                    stockDto.getProductTitle(),
                    request.getQuantity(),
                    stockDto.getHotDealPrice()));
        }

        // 요청 수량보다 재고가 부족한 요청이 존재하면 예외 처리
        if(!insufficientStockIds.isEmpty()){
            log.debug("요청 수량보다 재고가 부족합니다. hotDealProductIds = {}", insufficientStockIds);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_STOCK_NOT_ENOUGH, insufficientStockIds);
        }
        return responseDtos;
    }


    // hotDealProduct 재고 감소, 재고 감소 가능 검증
    private List<HotDealProductStockUpdateResponseDto> decreaseStockAndValidateAndConvertResponseDto(List<Long> hotDealProductIds,
                                                                                                     List<HotDealProductStockUpdateRequestDto> requestDtos) {
        // hotDealProducts 조회, hotDealProduct 존재 검증
        List<HotDealProduct> hotDealProducts = fetchHotDealProductsAndValidate(hotDealProductIds);

        // return 에 사용할 list
        List<HotDealProductStockUpdateResponseDto> responseDtos = new ArrayList<>();

        // 재고 감소 처리
        for (HotDealProduct hotDealProduct : hotDealProducts) {
            requestDtos.stream().filter(dto -> dto.getHotDealProductId().equals(hotDealProduct.getId()))
                    .findFirst()
                    .ifPresent(request -> {
                        Integer requestedQuantity = request.getQuantity();
                        Integer originalStock = hotDealProduct.getStock();
                        log.info("재고 감소 전 >>> hotDealProductId = {}, 조회 재고 = {}", hotDealProduct.getProductId(), originalStock);

                        // 재고 감소 (예외시 exception)
                        hotDealProduct.decreaseStock(requestedQuantity);

                        Integer remainingStock = hotDealProduct.getStock();
                        log.info("재고 감소 후 >>> hotDealProductId = {}, 재고 감소 = {}, 반영 재고 = {}", hotDealProduct.getProductId(), requestedQuantity, remainingStock);

                        // return list 에 add
                        responseDtos.add(new HotDealProductStockUpdateResponseDto(
                                request.getHotDealId(),
                                hotDealProduct.getId(),
                                hotDealProduct.getProductTitle(),
                                requestedQuantity,
                                originalStock,
                                remainingStock
                        ));
                    });
        }
        return responseDtos;
    }

    // hotDealProduct 재고 증가, 재고 증가 가능 검증
    private List<HotDealProductStockUpdateResponseDto> increaseStockAndValidateAndConvertResponseDto(List<Long> hotDealProductIds,
                                                                                                     List<HotDealProductStockUpdateRequestDto> requestDtos) {
        // hotDealProducts 조회, hotDealProduct 존재 검증
        List<HotDealProduct> hotDealProducts = fetchHotDealProductsAndValidate(hotDealProductIds);

        // return 에 사용할 list
        List<HotDealProductStockUpdateResponseDto> responseDtos = new ArrayList<>();

        // 재고 감소 처리
        for (HotDealProduct hotDealProduct : hotDealProducts) {
            requestDtos.stream().filter(dto -> dto.getHotDealProductId().equals(hotDealProduct.getId()))
                    .findFirst()
                    .ifPresent(request -> {
                        Integer requestedQuantity = request.getQuantity();
                        Integer originalStock = hotDealProduct.getStock();
                        log.info("재고 증가 전 >>> hotDealProductId = {}, 조회 재고 = {}", hotDealProduct.getProductId(), originalStock);

                        // 재고 증가
                        hotDealProduct.increaseStock(requestedQuantity);

                        Integer remainingStock = hotDealProduct.getStock();
                        log.info("재고 증가 후 >>> hotDealProductId = {}, 재고 증가 = {}, 반영 재고 = {}", hotDealProduct.getProductId(), requestedQuantity, remainingStock);

                        // return list 에 add
                        responseDtos.add(new HotDealProductStockUpdateResponseDto(
                                request.getHotDealId(),
                                hotDealProduct.getId(),
                                hotDealProduct.getProductTitle(),
                                requestedQuantity,
                                originalStock,
                                remainingStock
                        ));
                    });
        }
        return responseDtos;
    }

    // HotDealProductStockUpdateRequestDto 에서 hotDealIds 추출
    private List<Long> extractHotDealIdsFromUpdateDto(List<HotDealProductStockUpdateRequestDto> requestDtos) {
        return requestDtos.stream()
                .map(HotDealProductStockUpdateRequestDto::getHotDealId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // HotDealProductStockCheckRequestDto 에서 hotDealProductIds 추출
    private List<Long> extractHotDealProductIdsFromStockCheckRequestDto(List<HotDealProductStockCheckRequestDto> requestDtos) {
        return requestDtos.stream()
                .map(HotDealProductStockCheckRequestDto::getHotDealProductId)
                .sorted()
                .collect(Collectors.toList());
    }

    // hotDeal 조회, 존재 검증, 주문 가능 검증
    private List<HotDeal> fetchHotDealAndValidateHotDealAndOrderable(List<Long> hotDealIds) {

        // hotDeal 조회
        List<HotDeal> hotDeals = hotDealRepository.findByIds(hotDealIds);

        // 요청된 hotDeal 중 존재하지 않는 hotDeal 필터링 => 디버깅, 에러 처리 용도
        List<Long> noneMatchedHotDealIds = hotDealIds.stream()
                .filter(id -> hotDeals.stream().noneMatch(hotDeal -> hotDeal.getId().equals(id)))
                .collect(Collectors.toList());

        // 조회된 값 여부 확인
        if (!noneMatchedHotDealIds.isEmpty()) {
            log.debug("요청된 핫딜이 존재하지 않습니다. hotDealId = {}", noneMatchedHotDealIds);
            throw new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND, noneMatchedHotDealIds);
        }

        // hotDeal 활성화 여부 확인
        List<Long> noneActiveHotDealIds = hotDeals.stream()
                .filter(hotDeal -> !hotDeal.canOrder())
                .map(HotDeal::getId)
                .collect(Collectors.toList());

        if (!noneActiveHotDealIds.isEmpty()) {
            log.debug("활성화 된 핫딜이 아닙니다. hotDealId = {}", noneActiveHotDealIds);
            throw new HotDealException(ErrorCode.HOTDEAL_NON_ACTIVE, noneActiveHotDealIds);
        }
        return hotDeals;
    }

    // hotDeal 조회, 검증 (핫딜 주문 가능 상태 미확인)
    private List<HotDeal> fetchHotDealWithProductsAndValidateForIncrease(List<Long> hotDealIds) {

        // hotDeal 조회
        List<HotDeal> hotDeals = hotDealRepository.findByIds(hotDealIds);

        // 요청된 hotDeal 중 존재하지 않는 hotDeal 필터링 => 디버깅, 에러 처리 용도
        List<Long> noneMatchedHotDealIds = hotDealIds.stream()
                .filter(id -> hotDeals.stream()
                        .noneMatch(hotDeal -> hotDeal.getId().equals(id)))
                .collect(Collectors.toList());

        // 조회된 값 여부 확인
        if (!noneMatchedHotDealIds.isEmpty()) {
            log.debug("요청된 핫딜이 존재하지 않습니다. hotDealProductId = {}", noneMatchedHotDealIds);
            throw new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND, noneMatchedHotDealIds);
        }
        return hotDeals;
    }
    // hotDealProducts => ProductStockDto
    private List<HotDealProductStockDto> convertToHotDealProductStockDto(List<HotDealProduct> foundHotDealProducts) {
        return foundHotDealProducts.stream()
                .map(hp -> new HotDealProductStockDto(
                        hp.getId(),
                        hp.getProductId(),
                        hp.getProductTitle(),
                        hp.getStock(),
                        hp.getHotDealPrice()))
                .collect(Collectors.toList());
    }

}
