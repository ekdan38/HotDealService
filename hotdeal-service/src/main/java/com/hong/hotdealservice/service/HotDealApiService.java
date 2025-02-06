package com.hong.hotdealservice.service;

import com.hong.common.dto.*;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealException;
import com.hong.common.exception.custom.HotDealProductException;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
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

    // 핫딜 상품 조회, 재고 확인
    public List<HotDealProductStockCheckResponseDto> fetchProductAndValidateStock(List<HotDealProductStockCheckRequestDto> requestDtos) {
        // 요청 Dto 에서 hotDealId 추출
        List<Long> hotDealIds = extractHotDealIdsFromCheckDto(requestDtos);

        // hotDeal 조회, 검증
        List<HotDeal> hotDeals = fetchHotDealWithProductsAndValidate(hotDealIds);

        // hotDealProducts 요청 검증 Dto 변환
        return validateRequestedHotDealProductsAndConvertDto(hotDeals, requestDtos);
    }


    // 핫딜 상품 재고 감소
    @Transactional
    public List<HotDealProductStockUpdateResponseDto> decreaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos) {
        // 요청 dto 에서 hotDealProductIds 추출
        List<Long> hotDealProductIds = extractHotDealProductIdsFromUpdateDto(requestDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(hotDealProductIds);

        // 요청 dto 에서 hotDealIds 추출
        List<Long> hotDealIds = extractHotDealIdsFromUpdateDto(requestDtos);
        // hotDeal 조회, 검증
        List<HotDeal> hotDeals = fetchHotDealWithProductsAndValidate(hotDealIds);

        // hotDealProducts 요청 검증, 재고 감소
        List<HotDealProductStockUpdateResponseDto> responseDtos = decreaseStockAndValidateAndConvertResponseDto(hotDeals, requestDtos);
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
    public  List<HotDealProductStockUpdateResponseDto> increaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos) {
        // 요청 dto 에서 hotDealProductIds 추출
        List<Long> hotDealProductIds = extractHotDealProductIdsFromUpdateDto(requestDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(hotDealProductIds);

        // 요청 dto 에서 hotDealIds 추출
        List<Long> hotDealIds = extractHotDealIdsFromUpdateDto(requestDtos);
        // hotDeal 조회, 검증
        List<HotDeal> hotDeals = fetchHotDealWithProductsAndValidateForIncrease(hotDealIds);

        // hotDealProducts 요청 검증, 재고 증가
        List<HotDealProductStockUpdateResponseDto> responseDtos = increaseStockAndValidateAndConvertResponseDto(hotDeals, requestDtos);
        // 락 해제
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
        List<Long> hotDealIds = requestDtos.stream()
                .map(HotDealProductStockCheckRequestDto::getHotDealId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return hotDealIds;
    }

    // hotDealProductIds 추출
    private List<Long> extractHotDealProductIdsFromUpdateDto(List<HotDealProductStockUpdateRequestDto> requestDtos) {
        List<Long> hotDealProductIds = requestDtos.stream()
                .map(HotDealProductStockUpdateRequestDto::getHotDealProductId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return hotDealProductIds;
    }


    // hotDealProducts 요청 검증 Dto 변환
    private List<HotDealProductStockCheckResponseDto> validateRequestedHotDealProductsAndConvertDto(List<HotDeal> hotDeals,
                                                                                                    List<HotDealProductStockCheckRequestDto> requestDtos) {

        // requestDto hotDealId 기준 Map 변환
        Map<Long, List<HotDealProductStockCheckRequestDto>> hotDealProductMap = requestDtos.stream()
                .collect(Collectors.groupingBy(HotDealProductStockCheckRequestDto::getHotDealId));

        List<HotDealProductStockCheckResponseDto> hotDealProductStockCheckResponseDtos = new ArrayList<>();

        List<Long> insufficientStockIds = new ArrayList<>();
        for (HotDeal hotDeal : hotDeals) {
            // hotDealPorudctId 랑 dto의 productId랑 일치 하는 hotDelProduct 추출
            List<HotDealProductStockCheckRequestDto> dtos = hotDealProductMap.get(hotDeal.getId());

            // hotDealPorudctId 랑 dto의 productId랑 일치 하지 않는 hotDelProduct 추출 => 디버깅, 에러 처리 용도
            List<Long> noneMathedHotDealProductIds = hotDeal.getHotDealProducts().stream()
                    .filter(hp -> dtos.stream()
                            .noneMatch(dto -> dto.getHotDealProductId().equals(hp.getId())))
                    .map(HotDealProduct::getId)
                    .collect(Collectors.toList());

            if (!noneMathedHotDealProductIds.isEmpty()) {
                log.debug("요청된 핫딜 상품이 존재하지 않습니다. = {}", noneMathedHotDealProductIds);
                throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, noneMathedHotDealProductIds);
            }

            // hotDealPorudctId 랑 dto의 productId랑 일치 하는 hotDelProduct 추출
            List<HotDealProduct> matchedProducts = hotDeal.getHotDealProducts().stream()
                    .filter(hp -> dtos.stream()
                            .anyMatch(dto -> dto.getHotDealProductId().equals(hp.getId())))
                    .collect(Collectors.toList());

            for (HotDealProduct hotDealProduct : matchedProducts) {
                // hotDealProduct 와 id 가 같은 request 추출
                HotDealProductStockCheckRequestDto matchedDto = dtos.stream()
                        .filter(dto -> dto.getHotDealProductId().equals(hotDealProduct.getId()))
                        .findFirst()
                        .get(); // 위 로직에서 검증 함으로 get 처리

                // 재고 부족 체크
                if (matchedDto.getQuantity() > hotDealProduct.getStock()) {
                    log.debug("요청 수량보다 재고가 부족합니다. hotDealProductId = {}, stock = {}, requestQuantity = {}"
                            , hotDealProduct.getId(), hotDealProduct.getStock(), matchedDto.getQuantity());
                    insufficientStockIds.add(hotDealProduct.getId());
                }
                // 응답 Dto 생성
                hotDealProductStockCheckResponseDtos.add(
                        new HotDealProductStockCheckResponseDto(
                                hotDeal.getId(),
                                hotDealProduct.getId(),
                                hotDealProduct.getProductId(),
                                hotDealProduct.getProductTitle(),
                                matchedDto.getQuantity(),
                                hotDealProduct.getHotDealPrice()));
            }

        }
        if (!insufficientStockIds.isEmpty()) {
            log.debug("요청 수량보다 재고가 부족합니다. hotDealProductIds = {}", insufficientStockIds);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_STOCK_NOT_ENOUGH, insufficientStockIds);
        }

        return hotDealProductStockCheckResponseDtos;
    }


    // hotDeal 재고 감소, hotDealProducts 요청 검증
    private List<HotDealProductStockUpdateResponseDto> decreaseStockAndValidateAndConvertResponseDto(List<HotDeal> hotDeals,
                                                                                                        List<HotDealProductStockUpdateRequestDto> requestDtos) {

        // hotDealId 기준 Map 변환
        Map<Long, List<HotDealProductStockUpdateRequestDto>> hotDealProductMap = requestDtos.stream()
                .collect(Collectors.groupingBy(HotDealProductStockUpdateRequestDto::getHotDealId));

        List<HotDealProductStockUpdateResponseDto> responseDtos = new ArrayList<>();

        for (HotDeal hotDeal : hotDeals) {

            // hotDealProductId 랑 dto의 productId랑 일치 하는 hotDelProducts 추출
            List<HotDealProductStockUpdateRequestDto> dtos = hotDealProductMap.get(hotDeal.getId());

            // hotDealProductId 와 dto의 productId랑 일치 하지 않는 hotDelProducts 추출 => 디버깅, 에러 처리 용도
            List<Long> noneMathHotDealProductIds = hotDeal.getHotDealProducts().stream()
                    .filter(hp -> dtos.stream()
                            .noneMatch(dto -> dto.getHotDealProductId().equals(hp.getId())))
                    .map(HotDealProduct::getId)
                    .collect(Collectors.toList());

            if (!noneMathHotDealProductIds.isEmpty()) {
                log.debug("요청된 핫딜 상품이 존재하지 않습니다. = {}", noneMathHotDealProductIds);
                throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, noneMathHotDealProductIds);
            }

            // hotDealProductId 와 dto의 productId랑 일치 하는 hotDelProducts 추출
            List<HotDealProduct> matchedHotDealProducts = hotDeal.getHotDealProducts().stream()
                    .filter(hp -> dtos.stream()
                            .anyMatch(dto -> dto.getHotDealProductId().equals(hp.getId())))
                    .collect(Collectors.toList());

            // hotDealProduct 재고 감소 처리
            for (HotDealProduct hotDealProduct : matchedHotDealProducts) {
                // 매칭 되는 requestDto 찾기
                dtos.stream().filter(dto -> dto.getHotDealProductId().equals(hotDealProduct.getId()))
                        .findFirst()
                        .ifPresent(request -> {
                            Integer requestedQuantity = request.getQuantity();
                            Integer originalStock = hotDealProduct.getStock();
                            log.info("재고 감소 전 >>> hotDealProductId = {}, 조회 재고 = {}", hotDealProduct.getProductId(), originalStock);

                            // 재고 감소 (예외시 exception)
                            hotDealProduct.decreaseStock(requestedQuantity);

                            Integer remainingStock = hotDealProduct.getStock();
                            log.info("재고 감소 후 >>> hotDealProductId = {}, 재고 감소 = {}, 반영 재고 = {}", hotDealProduct.getProductId(), requestedQuantity, remainingStock);

                            responseDtos.add(new HotDealProductStockUpdateResponseDto(
                                    hotDeal.getId(),
                                    hotDealProduct.getId(),
                                    hotDealProduct.getProductTitle(),
                                    requestedQuantity,
                                    originalStock,
                                    remainingStock
                            ));
                        });
            }
        }
        return responseDtos;
    }

    // hotDeal 재고 증가, hotDealProducts 요청 검증
    private List<HotDealProductStockUpdateResponseDto> increaseStockAndValidateAndConvertResponseDto(List<HotDeal> hotDeals,
                                                                                                     List<HotDealProductStockUpdateRequestDto> requestDtos) {

        // hotDealId 기준 Map 변환
        Map<Long, List<HotDealProductStockUpdateRequestDto>> hotDealProductMap = requestDtos.stream()
                .collect(Collectors.groupingBy(HotDealProductStockUpdateRequestDto::getHotDealId));

        List<HotDealProductStockUpdateResponseDto> responseDtos = new ArrayList<>();

        for (HotDeal hotDeal : hotDeals) {

            // hotDealProductId 랑 dto의 productId랑 일치 하는 hotDelProducts 추출
            List<HotDealProductStockUpdateRequestDto> dtos = hotDealProductMap.get(hotDeal.getId());

            // hotDealProductId 와 dto의 productId랑 일치 하지 않는 hotDelProducts 추출 => 디버깅, 에러 처리 용도
            List<Long> noneMathHotDealProductIds = hotDeal.getHotDealProducts().stream()
                    .filter(hp -> dtos.stream()
                            .noneMatch(dto -> dto.getHotDealProductId().equals(hp.getId())))
                    .map(HotDealProduct::getId)
                    .collect(Collectors.toList());

            if (!noneMathHotDealProductIds.isEmpty()) {
                log.debug("요청된 핫딜 상품이 존재하지 않습니다. = {}", noneMathHotDealProductIds);
                throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, noneMathHotDealProductIds);
            }

            // hotDealProductId 와 dto의 productId랑 일치 하는 hotDelProducts 추출
            List<HotDealProduct> matchedHotDealProducts = hotDeal.getHotDealProducts().stream()
                    .filter(hp -> dtos.stream()
                            .anyMatch(dto -> dto.getHotDealProductId().equals(hp.getId())))
                    .collect(Collectors.toList());

            // hotDealProduct 재고 증가 처리
            for (HotDealProduct hotDealProduct : matchedHotDealProducts) {
                // 매칭 되는 requestDto 찾기
                dtos.stream().filter(dto -> dto.getHotDealProductId().equals(hotDealProduct.getId()))
                        .findFirst()
                        .ifPresent(request -> {
                            Integer requestedQuantity = request.getQuantity();
                            Integer originalStock = hotDealProduct.getStock();
                            log.info("재고 증가 전 >>> hotDealProductId = {}, 조회 재고 = {}", hotDealProduct.getProductId(), originalStock);

                            // 재고 증가
                            hotDealProduct.increaseStock(requestedQuantity);

                            Integer remainingStock = hotDealProduct.getStock();
                            log.info("재고 증가 후 >>> hotDealProductId = {}, 재고 증가 = {}, 반영 재고 = {}", hotDealProduct.getProductId(), requestedQuantity, remainingStock);

                            responseDtos.add(new HotDealProductStockUpdateResponseDto(
                                    hotDeal.getId(),
                                    hotDealProduct.getId(),
                                    hotDealProduct.getProductTitle(),
                                    requestedQuantity,
                                    originalStock,
                                    remainingStock
                            ));
                        });
            }
        }
        return responseDtos;
    }

    // hotDealIds 추출
    private List<Long> extractHotDealIdsFromUpdateDto(List<HotDealProductStockUpdateRequestDto> requestDtos) {
        return requestDtos.stream()
                .map(HotDealProductStockUpdateRequestDto::getHotDealId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // hotDeal 조회, 검증 (핫딜 주문 가능 상태 확인)
    private List<HotDeal> fetchHotDealWithProductsAndValidate(List<Long> hotDealIds) {

        // hotDeal 조회 (fetchJoin 으로 hotDealProduct 까지 조회)
        List<HotDeal> hotDeals = hotDealRepository.findByIdsWithHotDealProducts(hotDealIds);

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

        // hotDeal 활성화 여부 확인
        List<Long> noneActiveHotDealIds = new ArrayList<>();
        for (HotDeal hotDeal : hotDeals) {
            if (!hotDeal.canOrder()) noneActiveHotDealIds.add(hotDeal.getId());
        }

        if (!noneActiveHotDealIds.isEmpty()) {
            log.debug("활성화 된 핫딜이 아닙니다. hotDealId = {}", noneActiveHotDealIds);
            throw new HotDealException(ErrorCode.HOTDEAL_NON_ACTIVE, noneActiveHotDealIds);
        }
        return hotDeals;
    }

    // hotDeal 조회, 검증 (핫딜 주문 가능 상태 미확인)
    private List<HotDeal> fetchHotDealWithProductsAndValidateForIncrease(List<Long> hotDealIds) {

        // hotDeal 조회 (fetchJoin 으로 hotDealProduct 까지 조회)
        List<HotDeal> hotDeals = hotDealRepository.findByIdsWithHotDealProducts(hotDealIds);

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

    // <hotDealId, <HotDealProductDto> 형태 Map 변환
    private Map<Long, List<HotDealProductCommonDto>> buildHotDealProductMap(List<HotDealProductCommonDto> hotDealProductCommonDtos) {
        return hotDealProductCommonDtos.stream()
                .collect(Collectors.groupingBy(HotDealProductCommonDto::getHotDealId));
    }

}
