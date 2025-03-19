package com.hong.hotdealservice.service;

import com.hong.common.dto.HotDealProductStockCheckRequestDto;
import com.hong.common.dto.HotDealProductStockCheckResponseDto;
import com.hong.common.dto.HotDealProductStockUpdateRequestDto;
import com.hong.common.dto.HotDealProductStockUpdateResponseDto;
import com.hong.common.exception.ErrorCode;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealApiService]")
@Transactional(readOnly = true)
public class HotDealApiService {

    private final HotDealProductRepository hotDealProductRepository;
    private final HotDealRepository hotDealRepository;
    private final RedissonClient redissonClient;
    private final HotDealRedisRepository hotDealRedisRepository;
    private final HotDealProductRedisRepository hotDealProductRedisRepository;

    // hotDealProducts 단순 조회
    public List<HotDealProductStockProjection> fetchStock(List<Long> hotDealProductIds){
        // DB 조회
        List<HotDealProductStockProjection> stockProjections = fetchStockAndValidate(hotDealProductIds);
        return stockProjections.stream().map(dto -> new HotDealProductStockProjection(dto.getId(), dto.getStock())).collect(toList());
    }

    // 핫딜 상품 조회, 재고 확인
    public List<HotDealProductStockCheckResponseDto> fetchHotDealProductsStockAndValidateStock(List<HotDealProductStockCheckRequestDto> requestDtos) {
        // 1. hotDealProductIds 추출
        List<Long> hotDealProductIds = extractHotDealProductIdsFromStockCheckRequestDto(requestDtos);
        // cacheHit hotDealProductCacheDtos
        List<HotDealProductCacheDto> cacheHitHotDealProductCacheDtos = new ArrayList<>();
        // cacheMiss hotDealProductCacheDtos
        List<HotDealProductCacheDto> cacheMissHotDealProductCacheDtos = new ArrayList<>();
        // cacheMiss hotDealProducts
        List<HotDealProduct> cacheMissHotDealProducts = new ArrayList<>();

        // 2. hotDealProducts 조회(Redis, DB)
        Map<Long, HotDealProductCacheDto> hotDealProductMap =
                fetchHotDealProducts(hotDealProductIds, cacheMissHotDealProducts, cacheHitHotDealProductCacheDtos, cacheMissHotDealProductCacheDtos);

        // 3. hotDeal 조회(Redis, DB) 및 존재, 주문 가능 검증
        fetchHotDealAndValidateHotDealAndOrderable(hotDealProductMap);

        // 4. hotDealProduct stock 조회
        Map<Long, Integer> stockMap = fetchStock(cacheHitHotDealProductCacheDtos, cacheMissHotDealProducts);

        // 5. 요청에 대해 stock 검증 및 dto 변환
        List<HotDealProductStockCheckResponseDto> responseDtos = validateStockAndConvertDto(stockMap, hotDealProductMap, requestDtos);

        // 6. cacheMissHotDealProducts Redis 저장
        hotDealProductRedisRepository.saveAllHotDealProductWithTTL(cacheMissHotDealProductCacheDtos);

        return responseDtos;
    }

    // 핫딜 상품 재고 감소
    @Transactional
    public List<HotDealProductStockUpdateResponseDto> decreaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos) {
        // 1. 요청 dto 에서 hotDealProductIds 추출
        List<Long> hotDealProductIds = extractHotDealProductIdsFromUpdateDto(requestDtos);
        // 2. 락 획득
        List<RLock> locks = acquireLocks(hotDealProductIds);
        // 3. hotDealProduct 재고 감소, 재고 감소 가능 검증
        List<HotDealProductStockUpdateResponseDto> responseDtos = decreaseStockAndValidateAndConvertResponseDto(hotDealProductIds, requestDtos);
        // 4. 트랜잭션 성공 유무 상관 없이 트랜잭션 종료 시점 무조건 락 해제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                releaseLocks(locks);
            }
        });
        return responseDtos;
    }

    // 핫딜 상품 재고 증가
    @Transactional
    public List<HotDealProductStockUpdateResponseDto> increaseStock(List<HotDealProductStockUpdateRequestDto> requestDtos) {
        // 1. 요청 dto 에서 hotDealProductIds 추출
        List<Long> hotDealProductIds = extractHotDealProductIdsFromUpdateDto(requestDtos);
        // 2. 락 획득
        List<RLock> locks = acquireLocks(hotDealProductIds);
        // 3. hotDealProduct 재고 증가, 재고 증가 가능 검증
        List<HotDealProductStockUpdateResponseDto> responseDtos = increaseStockAndValidateAndConvertResponseDto(hotDealProductIds, requestDtos);
        // 4. 트랜잭션 성공 유무 상관 없이 트랜잭션 종료 시점 무조건 락 해제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
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
        LocalDateTime start = LocalDateTime.now();
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
            Duration duration = Duration.between(start, LocalDateTime.now());
            long waitMillis = duration.toMillis();
            log.info("hotDealProductId : " + hotDealProductId + "의 락 획득 대기 시간 = {}", waitMillis);
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

    // hotDealProductIds 추출
    private List<Long> extractHotDealProductIdsFromUpdateDto(List<HotDealProductStockUpdateRequestDto> requestDtos) {
        return requestDtos.stream()
                .map(HotDealProductStockUpdateRequestDto::getHotDealProductId)
                .distinct()
                .sorted()
                .collect(toList());
    }

    // hotDealProducts 조회, hotDealProduct 존재 검증
    private List<HotDealProduct> fetchHotDealProductsAndValidate(List<Long> hotDealProductIds) {
        // hotDealProducts 조회
        List<Long> sortedHotDealProductIds = hotDealProductIds
                .stream()
                .sorted()
                .distinct()
                .toList();
        // hotDealProducts 조회
        List<HotDealProduct> foundHotDealProducts = hotDealProductRepository.findByIds(sortedHotDealProductIds);

        // 조회된 hotDealProductIds 추출
        Set<Long> foundHotDealProductIds = foundHotDealProducts.stream()
                .map(HotDealProduct::getId)
                .collect(toSet());

        // reqeust Ids 중에서 조회되지 않은 ids 추출
        List<Long> noneMatchedHotDealProductIds = sortedHotDealProductIds.stream()
                .filter(id -> !foundHotDealProductIds.contains(id))
                .collect(toList());

        // 존재 하지 않는 hotDealProduct 예외
        if (!noneMatchedHotDealProductIds.isEmpty()) {
            log.debug("요청된 핫딜 상품이 존재하지 않습니다. hotDealProductId = {}", noneMatchedHotDealProductIds);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, noneMatchedHotDealProductIds);
        }
        return foundHotDealProducts;
    }

    // hotDealProducts 재고 조회
    private Map<Long, Integer> fetchStock(List<HotDealProductCacheDto> cacheHitHotDealProductCacheDtos,
                                          List<HotDealProduct> cacheMissHotDealProducts) {

        // cacheHitHotDealProductCacheDtos 만 DB 조회(cacheMiss 는 이전에 조회 됨)
        // cacheHitHotDealProductCacheDtos => hotDealProductIds 추출
        List<Long> hotDealProductIds = cacheHitHotDealProductCacheDtos
                .stream()
                .map(hp -> hp.getHotDealProductId())
                .collect(toList());

        List<HotDealProductStockProjection> stockProjections = new ArrayList<>();
        if(!hotDealProductIds.isEmpty()){
            // DB 조회
            stockProjections.addAll(hotDealProductRepository.findStockByProductIds(hotDealProductIds));
        }

        Set<Long> retrievedIds = stockProjections.stream()
                .map(HotDealProductStockProjection::getId)
                .collect(Collectors.toSet());

        // DB 에서 조회 되지 않은 ids 추출
        List<Long> missingIds = hotDealProductIds.stream()
                .filter(id -> !retrievedIds.contains(id))
                .toList();

        // DB 에서 조회 되지 않은 hotDealProduct 예외 처리
        if (!missingIds.isEmpty()) {
            log.debug("요청된 핫딜 상품이 존재 하지 않습니다. hotDealProductId = {}", missingIds);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, missingIds);
        }

        // stockMap 변환
        Map<Long, Integer> stockMap = new HashMap<>();
        for (HotDealProductStockProjection projection : stockProjections) {
            stockMap.put(projection.getId(), projection.getStock());
        }
        for (HotDealProduct product : cacheMissHotDealProducts) {
            stockMap.put(product.getId(), product.getStock());
        }
        return stockMap;
    }

    private List<HotDealProductStockProjection> fetchStockAndValidate(List<Long> hotDealProductIds){
        List<Long> sortedIds = hotDealProductIds.stream().sorted().distinct().toList();
        List<HotDealProductStockProjection> stockProjections = hotDealProductRepository.findStockByProductIds(sortedIds);

        Set<Long> retrievedIds = stockProjections.stream()
                .map(HotDealProductStockProjection::getId)
                .collect(Collectors.toSet());

        // DB 에서 조회 되지 않은 ids 추출
        List<Long> missingIds = hotDealProductIds.stream()
                .filter(id -> !retrievedIds.contains(id))
                .toList();

        // DB 에서 조회 되지 않은 hotDealProduct 예외 처리
        if (!missingIds.isEmpty()) {
            log.debug("요청된 핫딜 상품이 존재 하지 않습니다. hotDealProductId = {}", missingIds);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, missingIds);
        }
        return stockProjections;
    }

    // hotDealProduct 조회(Redis, DB)
    private Map<Long, HotDealProductCacheDto> fetchHotDealProducts(List<Long> hotDealProductIds,
                                                                   List<HotDealProduct> cacheMissHotDealProducts,
                                                                   List<HotDealProductCacheDto> cacheHitHotDealProductCacheDtos,
                                                                   List<HotDealProductCacheDto> cacheMissHotDealPrdoucts){
        Map<Long, HotDealProductCacheDto> hotDealProductMap = new HashMap<>();
        List<Long> missedHotDealProductIds = new ArrayList<>();

        // Redis 조회
        cacheHitHotDealProductCacheDtos.addAll(fetchCacheHotDealProducts(hotDealProductIds, hotDealProductMap, missedHotDealProductIds));

        // CacheMiss 존재 하면 DB 조회
        // DB 조회
        if(!missedHotDealProductIds.isEmpty()) {
            cacheMissHotDealPrdoucts.addAll(fetchCacheMissedHotDealProducts(missedHotDealProductIds, hotDealProductMap, cacheMissHotDealProducts));
        }
        return hotDealProductMap;
    }

    // request 에 대한 재고 검증 및 dto 변환
    private List<HotDealProductStockCheckResponseDto> validateStockAndConvertDto(Map<Long, Integer> stockMap,
                                                                                 Map<Long, HotDealProductCacheDto> hotDealProductMap,
                                                                                 List<HotDealProductStockCheckRequestDto> requestDtos) {
        // return 에 사용할 list
        List<HotDealProductStockCheckResponseDto> responseDtos = new ArrayList<>();
        // 요청 수량보다 재고가 부족한 요청 모아둘 list
        List<Long> insufficientStockIds = new ArrayList<>();

        // 요청 Dto 순회 및 stock 검증
        for (HotDealProductStockCheckRequestDto request : requestDtos) {
            Integer stock = stockMap.get(request.getHotDealProductId());
            // 요청 수량 > 재고
            if (stock == null || request.getQuantity() > stock) {
                insufficientStockIds.add(request.getHotDealProductId());
            }
            else {
                HotDealProductCacheDto hotDealProductCacheDto = hotDealProductMap.get(request.getHotDealProductId());
                responseDtos.add(new HotDealProductStockCheckResponseDto(
                        hotDealProductCacheDto.getHotDealProductId(),
                        hotDealProductCacheDto.getProductTitle(),
                        request.getQuantity(),
                        hotDealProductCacheDto.getHotDealPrice()
                ));
            }
        }
        // 요청 수량 보다 재고가 부족한 요청이 존재 하면 예외 처리
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
                                hotDealProduct.getId(),
                                hotDealProduct.getProductTitle(),
                                requestedQuantity
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
                                hotDealProduct.getId(),
                                hotDealProduct.getProductTitle(),
                                requestedQuantity
                        ));
                    });
        }
        return responseDtos;
    }

    // HotDealProductStockCheckRequestDto 에서 hotDealProductIds 추출
    private List<Long> extractHotDealProductIdsFromStockCheckRequestDto(List<HotDealProductStockCheckRequestDto> requestDtos) {
        return requestDtos.stream()
                .map(HotDealProductStockCheckRequestDto::getHotDealProductId)
                .sorted()
                .distinct()
                .collect(toList());
    }

    // hotDeal 조회, 존재 검증, 주문 가능 검증
    private void fetchHotDealAndValidateHotDealAndOrderable(Map<Long, HotDealProductCacheDto> hotDealProductMap) {

        // 모든 HotDealProductCacheDto에서 hotDealId 추출
        List<Long> hotDealIds = hotDealProductMap.values().stream()
                .map(HotDealProductCacheDto::getHotDealId)
                .distinct()
                .sorted()
                .collect(toList());

        // Redis 에서 캐싱된 HotDealProducts 조회
        // <hotDealID, cachedData>
        HashMap<Long, Object> cacheMap = new HashMap<>();
        // cacheMiss HotDealIds
        List<Long> cacheMissHotDealIds = new ArrayList<>();

        // Redis 조회
        List<HotDealCacheDto> cacheHitHotDeals = fetchCachedHotDeals(hotDealIds, cacheMap, cacheMissHotDealIds);

        // CacheMiss 존재 하면 DB 조회 후 Redis 에 MultiSet
        List<HotDeal> cacheMissHotDeals = new ArrayList<>();
        if(!cacheMissHotDealIds.isEmpty()){
            cacheMissHotDeals = fetchCacheMissedHotDealsAndSetRedis(cacheMissHotDealIds, cacheHitHotDeals, cacheMap);
        }

        // 주문 가능 검증
        validateOrderable(cacheMissHotDeals, cacheHitHotDeals);
    }

    private void validateOrderable(List<HotDeal> cacheMissHotDeals,
                                   List<HotDealCacheDto> cacheHitHotDeals){
        // hotDeal 관련 상품 주문 가능 여부 확인
        List<Long> noneActiveHotDealIds = cacheMissHotDeals.stream()
                .filter(hotDeal -> !hotDeal.canOrder())
                .map(HotDeal::getId)
                .collect(toList());

        cacheHitHotDeals.forEach(hotDeal -> {
            if(!canOrderForCacheHitHotDeal(hotDeal.getStartTime(), hotDeal.getEndTime())){
                noneActiveHotDealIds.add(hotDeal.getHotDealId());
            }
        });

        // 주문 가능 상태가 아닌 hotDeal 포함 되어 예외 처리
        if (!noneActiveHotDealIds.isEmpty()) {
            List<Long> sortedHotDealIds = noneActiveHotDealIds.stream().sorted().collect(toList());
            log.debug("활성화 된 핫딜이 아닙니다. hotDealId = {}", sortedHotDealIds);
            throw new HotDealException(ErrorCode.HOTDEAL_NON_ACTIVE, sortedHotDealIds);
        }
    }

    private List<HotDeal> fetchCacheMissedHotDealsAndSetRedis(List<Long> cacheMissHotDealIds, List<HotDealCacheDto> cacheHitHotDeals, HashMap<Long, Object> cacheMap) {
        // DB cacheMissHotDeals 조회
        List<HotDeal> fetchedHotDeals = hotDealRepository.findByIds(cacheMissHotDealIds);

        // 존재 하지 않는 hotDeal 필터링 => 디버깅, 에러 처리 용도
        List<Long> noneMatchedHotDealIds = cacheMissHotDealIds.stream()
                .filter(id -> fetchedHotDeals.stream().noneMatch(hotDeal -> hotDeal.getId().equals(id)))
                .collect(toList());

        // 존재 하지 않는 hotDeal 예외 처리(redis + DB 에 없음)
        if (!noneMatchedHotDealIds.isEmpty()) {
            log.debug("요청된 핫딜이 존재하지 않습니다. hotDealId = {}", noneMatchedHotDealIds);
            throw new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND, noneMatchedHotDealIds);
        }

        List<HotDealCacheDto> HotDealCacheDtos = new ArrayList();
        for (HotDeal h : fetchedHotDeals) {
            HotDealCacheDto hotDealCacheDto = new HotDealCacheDto(h);
            cacheMap.put(hotDealCacheDto.getHotDealId(), hotDealCacheDto);
            HotDealCacheDtos.add(hotDealCacheDto);
        }

        // Redis 저장
        hotDealRedisRepository.saveAllHotDealWithTTL(HotDealCacheDtos);

        return fetchedHotDeals;
    }

    private List<HotDealCacheDto> fetchCachedHotDeals(List<Long> hotDealIds, HashMap<Long, Object> cacheMap, List<Long> cacheMissHotDealIds) {

        // Redis 조회
        List<HotDealCacheDto> cachedHotDeals = hotDealRedisRepository.findAllHotDealByIds(hotDealIds);

        // cacheHit HotDeals 저장
        List<HotDealCacheDto> cacheHitHotDeals = new ArrayList<>();

        // cacheHit, cacheMiss 정리
        for(int i = 0; i < cachedHotDeals.size(); i++){
            HotDealCacheDto cachedData = cachedHotDeals.get(i);
            // cacheHit
            if (cachedData != null){
                cacheMap.put(hotDealIds.get(i), cachedData);
                cacheHitHotDeals.add(cachedData);
            }
            // cacheMiss
            else cacheMissHotDealIds.add(hotDealIds.get(i));
        }
        return cacheHitHotDeals;
    }

    public boolean canOrderForCacheHitHotDeal(LocalDateTime startTime, LocalDateTime endTime){
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    // Redis 에서 캐싱된 HotDealProducts 조회
    private List<HotDealProductCacheDto> fetchCacheHotDealProducts(List<Long> hotDealProductIds,
                                                                   Map<Long, HotDealProductCacheDto> hotDealProductMap,
                                                                   List<Long> missedProductIds) {

        List<Long> sortedHotDealProductIds = hotDealProductIds
                .stream()
                .sorted()
                .distinct()
                .toList();

        // Redis 조회
        List<HotDealProductCacheDto> cachedHotDealProducts = hotDealProductRedisRepository.findAllHotDealProductByIds(sortedHotDealProductIds);

        List<HotDealProductCacheDto> cacheHitHotDealProducts = new ArrayList<>();

        // cacheHit, cacheMiss 데이터 정리
        for (int i = 0; i < sortedHotDealProductIds.size(); i++) {
            HotDealProductCacheDto cachedData = cachedHotDealProducts.get(i);
            // cacaheHit
            if (cachedData != null) {
                hotDealProductMap.put(sortedHotDealProductIds.get(i), cachedData);
                cacheHitHotDealProducts.add(cachedData);
            }
            // cacheMiss
            else missedProductIds.add(sortedHotDealProductIds.get(i));
        }
        return cacheHitHotDealProducts;
    }
    // CacheMiss DB 조회
    private List<HotDealProductCacheDto> fetchCacheMissedHotDealProducts(List<Long> missedProductIds,
                                                                        Map<Long, HotDealProductCacheDto> hotDealProductMap,
                                                                         List<HotDealProduct> cacheMissHotDealProducts) {
        // DB 조회
        List<HotDealProduct> foundHotDealProudcts = hotDealProductRepository.findByIds(missedProductIds);
        cacheMissHotDealProducts.addAll(foundHotDealProudcts);


        // DB 에도 존재 하지 않는 hotDealProducts 예외 처리
        if (foundHotDealProudcts.size() != missedProductIds.size()) {
            List<Long> noneMatchedIds = missedProductIds.stream()
                    .filter(id -> foundHotDealProudcts.stream().noneMatch(hp -> hp.getId().equals(id)))
                    .collect(toList());
            log.debug("요청된 핫딜 상품이 존재하지 않습니다. productIds = {}", noneMatchedIds);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, noneMatchedIds);
        }

        // HotDealProductCacheDto 로 변환 및 hotDealProductMap 에 put
        ArrayList<HotDealProductCacheDto> cacheMissHotDealProductCacheDtos = new ArrayList<>();
        for (HotDealProduct hp : foundHotDealProudcts) {
            HotDealProductCacheDto cacheMissHotDealProductCacheDto = new HotDealProductCacheDto(hp);
            cacheMissHotDealProductCacheDtos.add(cacheMissHotDealProductCacheDto);
            hotDealProductMap.put(cacheMissHotDealProductCacheDto.getHotDealProductId(), cacheMissHotDealProductCacheDto);
        }
        return cacheMissHotDealProductCacheDtos;
    }
}
