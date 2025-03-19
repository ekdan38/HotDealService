package com.hong.productservice.service.product;

import com.hong.common.dto.ProductStockCheckRequestDto;
import com.hong.common.dto.ProductStockCheckResponseDto;
import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.ProductException;
import com.hong.productservice.domain.Product;
import com.hong.productservice.dto.category.CategoryDto;
import com.hong.productservice.dto.product.ProductCacheDto;
import com.hong.productservice.dto.product.ProductStockDto;
import com.hong.productservice.dto.product.ProductStockProjection;
import com.hong.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[ProductApiService]")
@Transactional(readOnly = true)
public class ProductApiService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;

    // product 단건 조회
    public Product getProduct(Long productId) {
        // 1. product 조회 및 검증
        return fetctProductByIdAndValidate(productId);
    }

    // products 조회, 검증
    private List<Product> fetchProductsAndValidate(List<Long> productIds) {
        // 1. products 조회 및 검증
        return fetchProductsByIdsAndValidate(productIds);
    }

    // product cache 조회(cacheAside) 및 stock 조회
    public List<ProductStockDto> getProductsWithStock(List<Long> productIds){

        // 조회 된 product 정리
        Map<Long, ProductCacheDto> ProductMap = new HashMap<>();
        // cacheHit ProductIds
        List<Long> cacheHitProductIds = new ArrayList<>();
        // cacheMiss ProductIds
        List<Long> cacheMissProductIds = new ArrayList<>();

        // 1. product 조회(Redis)
        // productIds 정렬
        List<Long> sortedProductIds = productIds.stream()
                .sorted()
                .distinct()
                .collect(Collectors.toList());
        fetchCachedProductData(sortedProductIds, ProductMap, cacheMissProductIds, cacheHitProductIds);

        // 2. cacheMissProductIds 존재 하면 DB 조회 후 Redis 에 MultiSet
        List<Product> cacheMissProducts = new ArrayList<>();
        if(!cacheMissProductIds.isEmpty()){
            cacheMissProducts.addAll(fetchAndCacheMissedProducts(cacheMissProductIds, ProductMap));
        }

        // 3. Stock DB 조회
        Map<Long, Integer> stockMap = fetchProductStock(cacheHitProductIds, cacheMissProducts);

        // ProductStockDto 변환
        return convertProductStockResponse(sortedProductIds, ProductMap, stockMap);
    }

    // products 조회, 재고 확인
    public List<ProductStockCheckResponseDto> fetchProductAndValidateStock(List<ProductStockCheckRequestDto> requestDtos) {
        // 1. productId 추출
        List<Long> productIds = extractProductIdsFromCheckDto(requestDtos);
        // 2. product 조회(cacheAside), stock DB 조회 및 검증
        List<ProductStockDto> productStocks = getProductsWithStock(productIds);
        // dto 변환
        return validateRequestAndConvertDto(requestDtos, productStocks);
    }

    // product 재고 감소
    @Transactional
    public List<ProductStockUpdateResponseDto> decreaseStock(List<ProductStockUpdateRequestDto> requestDtos) {
        // 1. productIds 추출
        List<Long> productIds = extractProductIdsFromUpdateDto(requestDtos);
        // 2. 락 획득
        List<RLock> locks = acquireLocks(productIds);
        // 3. products 조회 및 검증
        List<Product> products = fetchProductsAndValidate(productIds);
        // 4. product 재고 감소
        List<ProductStockUpdateResponseDto> responseDtos = decreaseStockAndConvertResponseDtos(requestDtos, products);
        // 5. 트랜잭션 성공 유무 상관 없이 트랜잭션 종료 시점 무조건 락 해제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                releaseLocks(locks);
            }
        });
        // 6. 응답 Dto 변환
        return responseDtos;
    }

    // product 재고 증가
    @Transactional
    public List<ProductStockUpdateResponseDto> increaseStock(List<ProductStockUpdateRequestDto> requestDtos) {
        // 1. productIds 추출
        List<Long> productIds = extractProductIdsFromUpdateDto(requestDtos);
        // 2. 락 획득
        List<RLock> locks = acquireLocks(productIds);
        // 3. products 조회 및 검증
        List<Product> products = fetchProductsAndValidate(productIds);
        // 4. product 재고 증가
        List<ProductStockUpdateResponseDto> responseDtos = increaseStockAndConvertDtos(requestDtos, products);
        // 5. 트랜잭션 성공 유무 상관 없이 트랜잭션 종료 시점 무조건 락 해제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                releaseLocks(locks);
            }
        });
        // 6. 응답 Dto 변환
        return responseDtos;
    }

    private Product fetctProductByIdAndValidate(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.debug("요청된 상품이 존재하지 않습니다. productId = {}", productId);
                    return new ProductException(ErrorCode.PRODUCT_NOT_FOUND, productId);
                });
    }

    private List<Product> fetchProductsByIdsAndValidate(List<Long> productIds) {
        // productIds 정렬
        Collections.sort(productIds);
        // product 조회
        List<Product> products = productRepository.findByIds(productIds);

        // 요청과, 조회된 product 와 다른 productIds 추출(디버깅, 예외 처리용)
        List<Long> noneMathProductIds = productIds.stream()
                .filter(id -> products.stream()
                        .noneMatch(product -> product.getId().equals(id)))
                .collect(Collectors.toList());

        if (!noneMathProductIds.isEmpty()) {
            log.debug("요청된 상품이 존재하지 않습니다. productId = {}", noneMathProductIds);
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND, noneMathProductIds);
        }
        return products;
    }

    // products 요청 검증 Dto 변환
    private List<ProductStockCheckResponseDto> validateRequestAndConvertDto(List<ProductStockCheckRequestDto> requestDtos,
                                                                            List<ProductStockDto> productStockDtos) {
        List<ProductStockCheckResponseDto> productStockCheckResponseDto = new ArrayList<>();
        // 재고 부족한 productId를 모아둘 리스트
        List<Long> insufficientStockProductIds = new ArrayList<>();

        // products Map 변환
        Map<Long, ProductStockDto> productMap = productStockDtos.stream()
                .collect(Collectors.toMap(ProductStockDto::getProductId, product -> product));

        // 상품과 요청된 정보 매핑해서 해당 상품이 재고가 충분한지 체크
        for (ProductStockCheckRequestDto dto : requestDtos) {
            ProductStockDto productStockDto = productMap.get(dto.getProductId());

            // 재고 부족 체크
            if(dto.getQuantity() > productStockDto.getStock()) {
                log.debug("요청 수량보다 재고가 부족합니다. productId = {}, stock = {}, requestQuantity = {}"
                        , productStockDto.getProductId(), productStockDto.getStock(), dto.getQuantity());
                insufficientStockProductIds.add(productStockDto.getProductId());
            }

            productStockCheckResponseDto.add(new ProductStockCheckResponseDto(
                    productStockDto.getProductId(),
                    productStockDto.getTitle(),
                    dto.getQuantity(),
                    productStockDto.getPrice()
            ));
        }
        if(!insufficientStockProductIds.isEmpty()){
            log.debug("요청 수량보다 재고가 부족합니다. productIds = {}", insufficientStockProductIds);
            throw new ProductException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH, insufficientStockProductIds);
        }
        return productStockCheckResponseDto;
    }


    // Redis 에서 캐싱된 데이터 조회
    private void fetchCachedProductData(List<Long> productIds,
                                        Map<Long, ProductCacheDto> cacheMap,
                                        List<Long> cacheMissProductIds,
                                        List<Long> cacheHitProductIds) {
        // Redis 조회 key 생성
        List<String> keys = productIds.stream()
                .map(id -> "getProduct::products:" + id)
                .collect(Collectors.toList());

        // Redis 조회
        List<Object> cachedProductStockDtos = redisTemplate.opsForValue().multiGet(keys);

        // cacheHit, cacheMiss 데이터 정리
        for (int i = 0; i < productIds.size(); i++) {
            ProductCacheDto cachedData = (ProductCacheDto) cachedProductStockDtos.get(i);
            // cacheHit
            if (cachedData != null) {
                cacheMap.put(productIds.get(i), cachedData);
                cacheHitProductIds.add(productIds.get(i));
            }
            // cacheMiss
            else {
                cacheMissProductIds.add(productIds.get(i));
            }
        }
    }

    // CacheMiss DB 조회, Redis MultiSet
    private List<Product> fetchAndCacheMissedProducts(List<Long> missedProductIds,
                                             Map<Long, ProductCacheDto> ProductMap) {
        // cacheMiss Product DB 조회
        List<Product> cacheMissedProducts = productRepository.findByIdsWithCategory(missedProductIds);

        // DB 에도 존재하지 않는 ID 예외 처리
        if (cacheMissedProducts.size() != missedProductIds.size()) {
            List<Long> noneMatchedIds = missedProductIds.stream()
                    .filter(id -> cacheMissedProducts.stream().noneMatch(product -> product.getId().equals(id)))
                    .collect(Collectors.toList());
            log.debug("요청된 상품이 존재하지 않습니다. productIds = {}", noneMatchedIds);
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND, noneMatchedIds);
        }

        Map<String, ProductCacheDto> newCacheEntries = new HashMap<>();

        // Redis에 저장할 데이터 정리
        for (Product product : cacheMissedProducts) {
            ProductCacheDto productCacheDto = new ProductCacheDto(
                    product.getId(),
                    product.getTitle(),
                    product.getPrice(),
                    product.getCategoryProducts().stream()
                            .map(cp -> new CategoryDto(cp.getCategory().getId(), cp.getCategory().getTitle()))
                            .collect(Collectors.toList())
            );

            ProductMap.put(product.getId(), productCacheDto);
            newCacheEntries.put("getProduct::products:" + product.getId(), productCacheDto);
        }

        // Redis에 MultiSet 저장
        redisTemplate.opsForValue().multiSet(newCacheEntries);

        // TTL 설정을 위해 Redis Pipeline 사용
        long ttl = 600L; // 10분 TTL
        redisTemplate.executePipelined((RedisCallback<Void>) connection -> {
            for (String key : newCacheEntries.keySet()) {
                connection.expire(key.getBytes(), ttl);
            }
            return null;
        });
        return cacheMissedProducts;
    }

    // Stock DB 조회
    private Map<Long, Integer> fetchProductStock(List<Long> cacheHitProductIds,
                                                 List<Product> cacheMissProducts) {

        // cacheHit Products 조회 (cacheMiss Products 는 조회 됨)
        List<ProductStockProjection> StockProjections = new ArrayList<>();
        if (!cacheHitProductIds.isEmpty()){
            // DB 조회
            StockProjections.addAll(productRepository.findStockByProductIds(cacheHitProductIds));
        }

        Set<Long> retrievedIds = StockProjections.stream()
                .map(ProductStockProjection::getGetId)
                .collect(Collectors.toSet());

        // DB 에서 조회 되지 않은 ids 추출
        List<Long> missingIds = cacheHitProductIds.stream()
                .filter(id -> !retrievedIds.contains(id))
                .toList();

        // DB 에서 조회 되지 않은 product 예외 처리
        if (!missingIds.isEmpty()) {
            log.debug("요청된 상품이 존재하지 않습니다. productId = {}", missingIds);
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND, missingIds);
        }

        // stockMap 변환
        Map<Long, Integer> stockMap = new HashMap<>();
        for (ProductStockProjection projection : StockProjections) {
            stockMap.put(projection.getGetId(), projection.getGetStock());
        }
        for (Product product : cacheMissProducts) {
            stockMap.put(product.getId(), product.getStock());
        }

        return stockMap;
    }

    // ProductStockDto 변환
    private List<ProductStockDto> convertProductStockResponse(List<Long> productIds,
                                                              Map<Long, ProductCacheDto> productMap,
                                                              Map<Long, Integer> stockMap) {
        List<ProductStockDto> responseDtos = new ArrayList<>();
        for (Long id : productIds) {
            ProductCacheDto info = productMap.get(id);
            Integer stock = stockMap.get(id);
            responseDtos.add(new ProductStockDto(info.getId(), info.getTitle(), info.getPrice(), stock));
        }
        return responseDtos;
    }

    // 재고 감소
    private List<ProductStockUpdateResponseDto> decreaseStockAndConvertResponseDtos(List<ProductStockUpdateRequestDto> requestDtos,
                                                                                    List<Product> products) {
        // map 변환
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        List<ProductStockUpdateResponseDto> responseDtos = new ArrayList<>();

        for (ProductStockUpdateRequestDto requestDto : requestDtos) {
            Product product = productMap.get(requestDto.getProductId());
            Integer requestedStock = requestDto.getQuantity();
            Integer originalStock = product.getStock();
            log.info("재고 감소 전 >>> productId = {}, 조회 재고 = {}", requestDto.getProductId(), originalStock);

            // product 재고 감소 (예외시 exception)
            product.decreaseStock(requestedStock);

            Integer remainingStock = product.getStock();
            log.info("재고 감소 후 >>> productId = {}, 재고 감소 = {}, 반영 재고 = {}", requestDto.getProductId(), requestedStock, remainingStock);

            responseDtos.add(new ProductStockUpdateResponseDto(
                    product.getId(),
                    product.getTitle(),
                    product.getPrice(),
                    requestedStock
            ));
        }
        return responseDtos;
    }


    // 재고 증가
    private List<ProductStockUpdateResponseDto> increaseStockAndConvertDtos(List<ProductStockUpdateRequestDto> requestDtos,
                                                                            List<Product> products) {
        // map 변환
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        List<ProductStockUpdateResponseDto> responseDtos = new ArrayList<>();

        for (ProductStockUpdateRequestDto requestDto : requestDtos) {
            Product product = productMap.get(requestDto.getProductId());
            Integer requestedStock = requestDto.getQuantity();
            Integer originalStock = product.getStock();
            log.info("재고 증가 전 >>> productId = {}, 조회 재고 = {}", requestDto.getProductId(), originalStock);

            // product 재고 증가
            product.increaseStock(requestedStock);

            Integer remainingStock = product.getStock();
            log.info("재고 증가 후 >>> productId = {}, 재고 증가 = {}, 반영 재고 = {}", requestDto.getProductId(), requestedStock, remainingStock);

            responseDtos.add(new ProductStockUpdateResponseDto(
                    product.getId(),
                    product.getTitle(),
                    product.getPrice(),
                    requestedStock
            ));
        }
        return responseDtos;
    }

    // ProductStockCheckRequestDto 에서 productId 추출
    private List<Long> extractProductIdsFromCheckDto(List<ProductStockCheckRequestDto> requestDtos) {
        return requestDtos.stream().map(ProductStockCheckRequestDto::getProductId).collect(Collectors.toList());
    }

    // ProductStockUpdateRequestDto 에서 productId 추출
    private List<Long> extractProductIdsFromUpdateDto(List<ProductStockUpdateRequestDto> requestDtos) {
        return requestDtos.stream().map(ProductStockUpdateRequestDto::getProductId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // 락 획득
    private List<RLock> acquireLocks(List<Long> productIds) {
        // 락 객체 목록 생성
        List<RLock> locks = new ArrayList<>();
        // 상품 락 생성
        LocalDateTime start = LocalDateTime.now();
        for (Long productId : productIds) {
            String lockKey = "product_lock:" + productId;
            log.info("락 획득 시도 key = {}", lockKey);
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean isLocked = lock.tryLock(10L, 10L, TimeUnit.SECONDS);
                if (!isLocked) {
                    log.error("락 획득 실패 key = {} ", lockKey);
                    throw new ProductException(ErrorCode.PRODUCT_LOCK_FAILED);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProductException(ErrorCode.PRODUCT_LOCK_FAILED);
            }
            locks.add(lock);
            log.info("락 획득 성공 key = {}", lockKey);
            Duration duration = Duration.between(start, LocalDateTime.now());
            long waitMillis = duration.toMillis();
            log.info("ProductId : " + productId + "의 락 획득 대기 시간 = {}", waitMillis);

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

    private List<ProductStockDto> convertToProductStockDto(List<Product> foundProducts){
        return foundProducts.stream()
                .map(p -> new ProductStockDto(p.getId(), p.getTitle(), p.getPrice(), p.getStock()))
                .collect(Collectors.toList());
    }
}
