package com.hong.productservice.service.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.ProductStockCheckRequestDto;
import com.hong.common.dto.ProductStockCheckResponseDto;
import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealProductException;
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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[ProductApiService]")
@Transactional(readOnly = true)
public class ProductApiService {

    private ObjectMapper objectMapper;
    private final RedisTemplate<String, ProductCacheDto> redisTemplate;
//    private final ProductStockCacheService cacheService;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;

    // product 단건 조회
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.debug("요청된 상품이 존재하지 않습니다. productId = {}", productId);
                    return new ProductException(ErrorCode.PRODUCT_NOT_FOUND, productId);
                });
    }

    // products 조회, 검증
    private List<Product> fetchProductsAndValidate(List<Long> productIds) {
        // productIds 정렬 (DB 조회시 IN절 약간의 성능 향상)
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


    public List<ProductStockDto> getProductsWithStock(List<Long> productIds){
        // productIds 정렬
        List<Long> sortedProductIds = productIds.stream()
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        Map<Long, ProductCacheDto> cacheMap = new HashMap<>();
        List<Long> missedProductIds = new ArrayList<>();
        // Redis 캐싱된 데이터 조회 및 정리
        fetchCachedProductData(sortedProductIds, cacheMap, missedProductIds);

        // CacheMiss 존재 하면 DB 조회 후 Redis 에 MultiSet
        if(!missedProductIds.isEmpty()){
            fetchAndCacheMissedProducts(missedProductIds, cacheMap);
        }

        // Stock DB 조회
        Map<Long, Integer> stockMap = fetchProductStock(sortedProductIds);

        // ProductStockDto 변환
        return convertProductStockResponse(sortedProductIds, cacheMap, stockMap);
    }

    // products 조회, 재고 확인
    public List<ProductStockCheckResponseDto> fetchProductAndValidateStock(List<ProductStockCheckRequestDto> requestDtos) {
        // productId 추출
        List<Long> productIds = extractProductIdsFromCheckDto(requestDtos);
        // product 조회, 검증
        List<ProductStockDto> productStocks = getProductsWithStock(productIds);
        // dto 변환
        return validateRequestAndConvertDto(requestDtos, productStocks);
    }

    // product 재고 감소
    @Transactional
    public List<ProductStockUpdateResponseDto> decreaseStock(List<ProductStockUpdateRequestDto> requestDtos) {
        // productIds 추출
        List<Long> productIds = extractProductIdsFromUpdateDto(requestDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(productIds);
        // proudcts 조회, 검증
        List<Product> products = fetchProductsAndValidate(productIds);
        // 재고 감소
        List<ProductStockUpdateResponseDto> responseDtos = decreaseStockAndConvertResponseDtos(requestDtos, products);
        // 트랜잭션 commit 후 락 해제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                releaseLocks(locks);
            }
        });
        return responseDtos;
    }

    // product 재고 증가
    @Transactional
    public List<ProductStockUpdateResponseDto> increaseStock(List<ProductStockUpdateRequestDto> requestDtos) {
        // productIds 추출
        List<Long> productIds = extractProductIdsFromUpdateDto(requestDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(productIds);
        // proudcts 조회, 검증
        List<Product> products = fetchProductsAndValidate(productIds);
        // 재고 감소
        List<ProductStockUpdateResponseDto> responseDtos = increaseStockAndConvertDtos(requestDtos, products);
        // 트랜잭션 commit 후 락 해제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                releaseLocks(locks);
            }
        });

        return responseDtos;
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
            throw new HotDealProductException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH, insufficientStockProductIds);
        }
        return productStockCheckResponseDto;
    }


    // Redis 에서 캐싱된 데이터 조회
    private void fetchCachedProductData(List<Long> productIds,
                                        Map<Long, ProductCacheDto> cacheMap,
                                        List<Long> missedProductIds) {
        // Redis 조회 key 생성
        List<String> keys = productIds.stream()
                .map(id -> "getProduct::products:" + id)
                .collect(Collectors.toList());

        // 캐싱된 데이터 조회
        List<ProductCacheDto> cachedProductStockDtos = redisTemplate.opsForValue().multiGet(keys);

        // cacheHit, cacheMiss 데이터 정리
        for (int i = 0; i < productIds.size(); i++) {
            ProductCacheDto cachedData = cachedProductStockDtos.get(i);
            if (cachedData != null) {
                cacheMap.put(productIds.get(i), cachedData);
            } else {
                missedProductIds.add(productIds.get(i));
            }
        }
    }

    // CacheMiss DB 조회, Redis MultiSet
    private void fetchAndCacheMissedProducts(List<Long> missedProductIds,
                                             Map<Long, ProductCacheDto> cacheMap) {
        // DB 조회
        List<Product> missedProducts = productRepository.findByIdsWithCategory(missedProductIds);

        // DB 에도 존재하지 않는 ID 예외 처리
        if (missedProducts.size() != missedProductIds.size()) {
            List<Long> noneMatchedIds = missedProductIds.stream()
                    .filter(id -> missedProducts.stream().noneMatch(product -> product.getId().equals(id)))
                    .collect(Collectors.toList());
            log.debug("요청된 상품이 존재하지 않습니다. productIds = {}", noneMatchedIds);
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND, noneMatchedIds);
        }

        Map<String, ProductCacheDto> newCacheEntries = new HashMap<>();

        // Redis에 저장할 데이터 정리
        for (Product product : missedProducts) {
            ProductCacheDto productCacheDto = new ProductCacheDto(
                    product.getId(),
                    product.getTitle(),
                    product.getPrice(),
                    product.getCategoryProducts().stream()
                            .map(cp -> new CategoryDto(cp.getCategory().getId(), cp.getCategory().getTitle()))
                            .collect(Collectors.toList())
            );

            cacheMap.put(product.getId(), productCacheDto);
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
    }
    // Stock DB 조회
    private Map<Long, Integer> fetchProductStock(List<Long> productIds) {
        return productRepository.findStockByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductStockProjection::getGetId, ProductStockProjection::getGetStock));
    }

    // ProductStockDto 변환
    private List<ProductStockDto> convertProductStockResponse(List<Long> productIds,
                                                              Map<Long, ProductCacheDto> cacheMap,
                                                              Map<Long, Integer> stockMap) {
        List<ProductStockDto> responseDtos = new ArrayList<>();
        for (Long id : productIds) {
            ProductCacheDto info = cacheMap.get(id);
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
                    requestedStock,
                    originalStock,
                    remainingStock
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
                    requestedStock,
                    originalStock,
                    remainingStock
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
