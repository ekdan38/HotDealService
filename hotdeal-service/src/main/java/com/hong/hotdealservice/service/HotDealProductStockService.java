package com.hong.hotdealservice.service;

import com.hong.common.dto.*;
import com.hong.common.dto.kafka.ExpiredOrderEventDto;
import com.hong.common.dto.kafka.RefundOrderEventDto;
import com.hong.common.dto.kafka.StockFinalizeEventDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealException;
import com.hong.common.exception.custom.HotDealProductException;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.domain.StockReservation;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import com.hong.hotdealservice.domain.status.ReserveStatus;
import com.hong.hotdealservice.dto.HotDealCacheDto;
import com.hong.hotdealservice.dto.ProductCacheDto;
import com.hong.hotdealservice.dto.projection.HotDealSimpleDto;
import com.hong.hotdealservice.dto.projection.ProductReservedQuantityDto;
import com.hong.hotdealservice.repository.*;
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

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealProductReserveService]")
public class HotDealProductStockService {

    private final HotDealProductRepository hotDealProductRepository;
    private final StockReservationRepository stockReservationRepository;
    private final HotDealRepository hotDealRepository;
    private final HotDealRedisRepository hotDealRedisRepository;
    private final ProductRedisRepository productRedisRepository;
    private final RedissonClient redissonClient;

    /**
     * order 생성 시 호출 되는 serviceLogic
     * product 점유
     */
    @Transactional
    public ProductReservationResponseDto reserveStock(ProductReservationRequestDto requestDto){
        // 1. request 에서 productIds 추출
        List<Long> productIds = extractProductIds(requestDto.getProducts());

        // 2. product 조회 및 검증 (response 에 product 정보 필요)
        Map<Long, ProductCacheDto> productMap = getProductsAndValidate(productIds);

        // 3. hotDeal 활성화 검증
        getHotDealsAndValidateOrderAble(productMap);

        // 4. stock 점유 (메서드 내부적으로 락 획득, 해제 로직 존재)
        return reserveStockAndConvertToResponse(requestDto, productMap, productIds);
    }

    /**
     * 결제 반영 후 orderStatus update 후 orderService 에서 호출 되는 serviceLogic
     * 최종 stock 반영
     * 1. 결제 성공 => Redis Stock 감소, reserveStock 내역 삭제
     * 2. 결제 실패 => reserveStock 내역 삭제
     */
    @Transactional
    public StockFinalizeResponseDto handleStockFinalization(StockFinalizeEventDto dto){
        String orderId = dto.getOrderId();
        // 1. orderId 기준 reserveStock 조회 및 검증
        List<StockReservation> reservations = getStockReservationsAndValidate(orderId);

        // 2. 최종 stock 반영
        return handleStockAndCovertToResponseDto(dto, reservations, orderId);
    }

    /**
     * reserveStock 점유 해제
     */
    @Transactional
    public ReleaseReservedStockResponseDto releaseReservedStocks(ExpiredOrderEventDto dto){
        String orderId = dto.getOrderId();

        // 1. 모든 reservation 조회
        List<StockReservation> allReservations = stockReservationRepository.findAllByOrderId(orderId);

        // 2. 점유 된 재고 테이블 존재 검증. 존재 하지 않으면 return
        if (allReservations.isEmpty()) {
            log.warn("예약된 재고가 존재하지 않습니다. 요청 orderId = {}", orderId);
            return new ReleaseReservedStockResponseDto(true, false);
        }

        // 3. 조회 한 reservations 에서 productId 추출
        List<Long> productIds = extractProductIdsFromReservations(allReservations);

        // 4. 락 획득
        List<RLock> rLocks = acquireLocks(productIds);

        // 5. reserveStock Status 변경
        try {
            for (StockReservation reservation : allReservations) {
                reservation.updateToCanceled();
            }
            log.info("ReserveStock 점유 해제. orderId = {}", orderId);
        }
        finally {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    releaseLocks(rLocks);
                }
            });
        }
        return new ReleaseReservedStockResponseDto(true, false);
    }

    /**
     *  재고 복구 (재고 반영, 재고 점유 테이블 상태 변경)
     *  핫딜 상품의 핫딜이 끝나서 DB와 재고 동기화 했다면 DB에 반영
     *  동기화 되지 않았다면 Redis에 반영
     */
    @Transactional
    public StockRestoreResponseDto restoreStock(RefundOrderEventDto dto){
        String orderId = dto.getOrderId();

        // 1. 재고 점유 테이블 조회
        List<StockReservation> allReservations = stockReservationRepository.findAllByOrderId(orderId);

        // 2. 재고 점유 테이블 존재 검증. 존재 하지 않으면 return
        if (allReservations.isEmpty()) {
            log.warn("점유 테이블에 내역이 존재하지 않습니다. orderIds = {}", orderId);
            return new StockRestoreResponseDto(true, false);
        }

        // 3. 재고 점유 내역에서 productId 추출
        List<Long> productIds = allReservations.stream().map(r -> r.getProductId()).toList();

        // 4. product 조회 및 hotDeal Fetch Join
        List<HotDealProduct> hotDealProducts = hotDealProductRepository.findByIdsWithHotdeal(productIds);

        // 5. productId, hotDealProduct 매핑(map 변환)
        Map<Long, HotDealProduct> productMap = hotDealProducts.stream()
                .collect(Collectors.toMap(HotDealProduct::getId, p -> p));

        // 6. 락 획득
        List<RLock> rLocks = acquireLocks(productIds);

        // 7. 재고 복구
        // product의 hotdeal이 isSynced 이면 DB 에 재고 반영
        // 아니라면 Redis에 재고 반영
        try{
            for (StockReservation reservation : allReservations) {
                Long productId = reservation.getProductId();
                Integer reservedQuantity = reservation.getReservedQuantity();
                HotDealProduct product = productMap.get(productId);

                Boolean isStockSynced = product.getHotDeal().getIsStockSynced();

                // 재고 DB 반영
                if(isStockSynced){
                    product.increaseStock(reservedQuantity);
                    log.info("DB 재고 복구 처리 완료. productId = {}, requestedQuantity = {}", product, reservedQuantity);
                }
                // 재고 Redis 반영
                else{
                    productRedisRepository.increaseStock(productId, reservedQuantity);
                    log.info("Redis 재고 복구 처리 완료. productId = {}, requestedQuantity = {}", product, reservedQuantity);
                }
                // 점유 상태 CANCELED 처리
                reservation.updateToCanceled();
            }
        }
        finally {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    releaseLocks(rLocks);
                }
            });
        }

        return new StockRestoreResponseDto(true, false);
    }


    private StockFinalizeResponseDto handleStockAndCovertToResponseDto(StockFinalizeEventDto dto, List<StockReservation> reservations, String orderId) {
        // 1. 락 획득
        List<Long> productIds = extractProductIdsFromStockFinalize(reservations);
        List<RLock> rLocks = acquireLocks(productIds);

        try{
            // 2. 최종 stock 반영
            // 결제 성공
            List<Long> successIds = new ArrayList<>();
            if(dto.isPaySuccess()){
                for (StockReservation reservation : reservations) {
                    // Redis 재고 감소
                    Long result = productRedisRepository.decreaseStockWithLua(reservation.getProductId(), reservation.getReservedQuantity());
                    // 이론상 재고 부족 발생 하지 않음. 다만, 혹시 모를 이유 대비해 방어 로직
                    if(result == null || result < 0) {
                        log.error("재고 부족 혹은 Redis 오류 발생. orderId = {}, productId = {}, reservedQuantity = {}",
                                orderId, reservation.getProductId(), reservation.getReservedQuantity());
                        // reserveStock ERROR 처리 => 추후 로깅, DB 데이터 추적으로 처리
                        reservation.updateToError();
                        successIds.add(reservation.getId());
                    }
                    else{
                        // reserveStock CONFIRMED 처리
                        reservation.updateToConfirmed();
                    }
                }
                // 재고 감소 실패건
                if(!successIds.isEmpty()){
                    log.error("재고 감소 실패가 존재합니다. productIds = {}", successIds);
                    throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_FAIL_DECREASE_STOCK, successIds);
                }
                else {
                    log.info("결제 성공 건 Redis 재고 감소 및 ReserveStock CONFIRMED 처리 완료. orderId = {}", orderId);
                }
            }

            // 결제 실패
            else{
                for (StockReservation reservation : reservations) {
                    // reserveStock CANCEL 처리
                    reservation.updateToCanceled();
                }
                log.info("결제 실패 건 ReserveStock 내역 CANCEL 처리 완료. orderId = {}", orderId);
            }
        }
        finally {
            // 3. 락 해제
            // 트랜잭션 성공 유무 상관 없이 트랜잭션 종료 시점 무조건 락 해제
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    releaseLocks(rLocks);
                }
            });
        }
        return new StockFinalizeResponseDto(true, false);
    }

    private List<Long> extractProductIdsFromStockFinalize(List<StockReservation> reservations) {
        return reservations.stream()
                .map(StockReservation::getProductId)
                .distinct()
                .sorted()
                .toList();
    }

    private List<StockReservation> getStockReservationsAndValidate(String orderId) {
        List<StockReservation> reservations = stockReservationRepository.findAllByOrderId(orderId);
        if(reservations.isEmpty()){
            log.error("이미 처리된 주문이거나 존재 하지 않습니다. orderId = {}", orderId);
            throw new HotDealProductException(ErrorCode.RESERVATION_NOT_FOUND, orderId);
        }
        return reservations;
    }

    private ProductReservationResponseDto reserveStockAndConvertToResponse(ProductReservationRequestDto requestDtos,
                                                                           Map<Long, ProductCacheDto> productMap,
                                                                           List<Long> productIds) {
        // 1. 락 획득
        List<RLock> rLocks = acquireLocks(productIds);
        try {
            // order 구분 하는 unique 한 order 값
            String reservationToken = UUID.randomUUID().toString();

            List<StockReservation> stockReservations = new ArrayList<>();
            List<ReservedProductDto> reservedProducts = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

            String orderId = requestDtos.getOrderId();

            // 2. 재고 점유 상태 기준 DB 조회
            ReserveStatus reserveStatus = ReserveStatus.RESERVED;
            List<ProductReservedQuantityDto> reservedStocks =
                    stockReservationRepository.sumReservedQuantityByProductId(productIds, reserveStatus);

            // Map 변환
            Map<Long, Long> reservedStockMap = new HashMap<>();
            for (Long productId : productIds) {
                reservedStockMap.put(productId, 0L);
            }
            for (ProductReservedQuantityDto dto : reservedStocks) {
                reservedStockMap.put(dto.getProductId(), dto.getReservedQuantity());
            }

            // 재고 부족한 상품 Id List
            ArrayList<Long> notEnoughStockProductIds = new ArrayList<>();

            // 3. 각 상품에 대한 재고 확인 및 점유
            for (ProductReservationDto request : requestDtos.getProducts()) {
                Long productId = request.getProductId();
                Integer quantity = request.getQuantity();

                // Redis 에서 stock 조회
                Integer redisStock = productRedisRepository.getStock(productId);
                if(redisStock == null){
                    log.debug("Redis 에 재고가 없습니다. productId = {}", productId);
                    throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND_STOCK_IN_REDIS);
                }

                // DB 예약 수량 조회 (Map에서)
                int reservedStock = reservedStockMap.get(productId).intValue();

                // 사용 가능한 재고 계산
                int availableStock = redisStock - reservedStock;
                if (availableStock < quantity) {
                    log.error("재고 부족. productId = {}, 요청 수량 = {}, 사용 가능 재고 = {}", productId, quantity, availableStock);
                    notEnoughStockProductIds.add(productId);
                    continue;
                }

                // stock 점유
                // stockReservation 생성
                stockReservations.add(StockReservation.create(
                        orderId,
                        reservationToken,
                        productId,
                        quantity,
                        now.plusMinutes(15))
                );

                // response
                ProductCacheDto productCacheDto = productMap.get(productId);
                reservedProducts.add(new ReservedProductDto(
                        productId,
                        productCacheDto.getTitle(),
                        quantity,
                        productCacheDto.getPrice()
                ));
            }
            // 재고 부족 예외 처리
            if(!notEnoughStockProductIds.isEmpty()){
                throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_ENOUGH_STOCK, notEnoughStockProductIds);
            }
            // stock 점유 save
            stockReservationRepository.saveAll(stockReservations);
            return new ProductReservationResponseDto(reservationToken, reservedProducts);
        }
        finally {
            // 트랜잭션 성공 유무 상관 없이 트랜잭션 종료 시점 무조건 락 해제
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    releaseLocks(rLocks);
                }
            });
        }
    }


    // 재고 감소
    public void decreaseStockInRedis(Long productId, Integer quantity) {
        Long result = productRedisRepository.decreaseStockWithLua(productId, quantity);
        log.info("재고 감소. productId = {}, quantity = {}", productId, quantity);
        if(result == null || result < 0){
            log.error("재고 부족 또는 Redis 오류. productId = {}, requestedQuantity = {}", productId, quantity);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_ENOUGH_STOCK, List.of(productId));
        }
    }

    // 분산 락 획득
    private List<RLock> acquireLocks(List<Long> productIds) {
        // 락 객체 목록 생성
        List<RLock> locks = new ArrayList<>();
        // 락 생성
        LocalDateTime start = LocalDateTime.now();

        for (Long productId : productIds) {
            String lockKey = "stock_lock:" + productId;
            log.info("락 획득 시도 key = {}", lockKey);
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean isLocked = lock.tryLock(3L, 5L, TimeUnit.SECONDS);
                if (!isLocked) {
                    Duration duration = Duration.between(start, LocalDateTime.now());
                    log.error("hotDealProduct = {} 에 대한 락 획득에 실패했습니다. 락 획득 대기 시간 = {}", productId, duration.toMillis());
                    throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_LOCK_FAILED, productId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("hotDealProduct = {} 에 대한 락 획득중 입터럽트가 발생했습니다.", productId);
                throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_LOCK_INTERRUPTED, productId);
            }
            locks.add(lock);
            log.info("락 획득 성공 key = {}", lockKey);
            Duration duration = Duration.between(start, LocalDateTime.now());
            long waitMillis = duration.toMillis();
            log.info("productId : " + productId + "의 락 획득 대기 시간 = {}", waitMillis);
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

    private Map<Long, ProductCacheDto> getProductsAndValidate(List<Long> productIds){
        Map<Long, ProductCacheDto> productMap = new HashMap<>();
        List<Long> cacheMissProducts = new ArrayList<>();
        // 1. Redis 조회
        List<ProductCacheDto> cachedProducts = productRedisRepository.findAllByIds(productIds);
        // 2. cacheHit, Miss 정리
        for(int i = 0; i < productIds.size(); i++){
            ProductCacheDto cachedData = cachedProducts.get(i);
            // cacheHit
            if(cachedData != null){
                productMap.put(productIds.get(i), cachedData);
            }
            // cacheMiss
            else {
                cacheMissProducts.add(productIds.get(i));
            }
        }

        // 3. cacheMiss 존재 하면 DB 조회 후 cache save
        if (!cacheMissProducts.isEmpty()){
            // DB 조회
            log.info("상품 재고 점유. [상품 조회 쿼리 실행] : productIds = {}", cacheMissProducts);
            List<HotDealProduct> foundProducts = hotDealProductRepository.findByIdIn(cacheMissProducts);
            log.info("상품 재고 점유. [상품 조회 쿼리 종료] : productIds = {}", cacheMissProducts);
            // DB 에 존재 하지 않는 product 예외 처리
            if(cacheMissProducts.size() != foundProducts.size()){
                List<Long> noneMatchIds = cacheMissProducts.stream()
                        .filter(id -> foundProducts.stream().noneMatch(hp -> hp.getId().equals(id)))
                        .toList();
                log.error("요청된 핫딜 상품이 존재하지 않습니다. productIds = {}", noneMatchIds);
                throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, noneMatchIds);
            }

            // productMap 에 정리 (cacheHit, cacheMiss 데이터 관리)
            List<ProductCacheDto> productsToCache = new ArrayList<>();
            foundProducts.forEach(hp -> {
                ProductCacheDto productCacheDto = new ProductCacheDto(hp);
                productsToCache.add(productCacheDto);
                productMap.put(hp.getId(), productCacheDto);
            });
           // 4. Redis Save
           productRedisRepository.saveAllWithTTL(productsToCache);
        }
        return productMap;
    }

    private void getHotDealsAndValidateOrderAble(Map<Long, ProductCacheDto> productMap){
        Map<Long, HotDealCacheDto> hotDealMap = new HashMap<>();
        List<Long> cacheMissIds = new ArrayList<>();

        // 1. hotDealIds 추출
        List<Long> hotDealIds = productMap.values().stream()
                .map(ProductCacheDto::getHotDealId)
                .distinct()
                .sorted()
                .toList();

        // 2. Redis 조회
        List<HotDealCacheDto> cachedHotDeals = hotDealRedisRepository.findAllByIds(hotDealIds);

        // 3. cacheHit, Miss 정리
        for(int i = 0; i < hotDealIds.size(); i++){
            HotDealCacheDto hotDealCacheDto = cachedHotDeals.get(i);
            // cacheHit
            if(hotDealCacheDto != null){
                hotDealMap.put(hotDealIds.get(i), hotDealCacheDto);
            }
            // cacheMiss
            else {
                cacheMissIds.add(hotDealIds.get(i));
            }
        }

        // 4. cacheMiss 존재 하면 DB 조회 후 cache save
        if (!cacheMissIds.isEmpty()){
            // DB 조회
            log.info("상품 재고 점유. [핫딜 조회 쿼리 실행] : hotDealIds = {}", cacheMissIds);
            List<HotDealSimpleDto> foundHotDeals = hotDealRepository.findByIds(cacheMissIds);
            log.info("상품 재고 점유. [핫딜 조회 쿼리 실행] : hotDealIds = {}", cacheMissIds);
            // DB 에 존재 하지 않는 product 예외 처리
            if(cacheMissIds.size() != foundHotDeals.size()){
                List<Long> noneMatchIds = cacheMissIds.stream()
                        .filter(id -> foundHotDeals.stream().noneMatch(h -> h.getId().equals(id)))
                        .toList();
                log.debug("핫딜이 존재하지 않습니다. hotDealId = {}", noneMatchIds);
                throw new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND, noneMatchIds);
            }
            // hotDealMap 에 정리 (cacheHit, cacheMiss 데이터 관리)
            List<HotDealCacheDto> hotDealsToCache = new ArrayList<>();
            foundHotDeals.forEach(h -> {
                HotDealCacheDto hotDealCacheDto = new HotDealCacheDto(h);
                hotDealsToCache.add(hotDealCacheDto);
                hotDealMap.put(h.getId(), hotDealCacheDto);
            });
            // Redis Save
            hotDealRedisRepository.saveAllWithTTL(hotDealsToCache);
        }

        // 5. hotDeal 활성화 검증
        List<Long> noneActiveHotDealIds = new ArrayList<>();
        hotDealMap.values().forEach(h -> {
            if(h.getStatus() != HotDealStatus.ACTIVE) noneActiveHotDealIds.add(h.getId());
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
            if(!(now.isAfter(h.getStartTime()) && now.isBefore(h.getEndTime()))){
                noneActiveHotDealIds.add(h.getId());
            }
        });
        // 주문 가능 상태가 아닌 hotDeal 포함 되어 예외 처리
        if (!noneActiveHotDealIds.isEmpty()) {
            List<Long> sortedHotDealIds = noneActiveHotDealIds.stream().sorted().collect(toList());
            log.error("활성화 된 핫딜이 아닙니다. hotDealId = {}", sortedHotDealIds);
            throw new HotDealException(ErrorCode.HOTDEAL_NON_ACTIVE, sortedHotDealIds);
        }
    }

    private List<Long> extractProductIds(List<ProductReservationDto> productDtos) {
        return productDtos.stream()
                .map(ProductReservationDto::getProductId)
                .sorted()
                .distinct()
                .toList();
    }
    private List<Long> extractProductIdsFromReservations(List<StockReservation> reservations) {
        return reservations.stream()
                .map(StockReservation::getProductId)
                .distinct()
                .sorted()
                .toList();
    }

}
