package com.hong.productservice.service.product;

import com.hong.common.dto.*;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealProductException;
import com.hong.common.exception.custom.ProductException;
import com.hong.productservice.domain.Product;
import com.hong.productservice.dto.product.ProductStockDto;
import com.hong.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[ProductApiService]")
@Transactional(readOnly = true)
public class ProductApiService {

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

    //  products 재고 조회
    public List<ProductStockDto> getProductStocks(List<Long> productIds){
        // 추후 캐싱
        List<Product> foundProducts = fetchProductsAndValidate(productIds);
        return convertToProductStockDto(foundProducts);
    }

    // products 조회, 재고 확인
    public List<ProductStockCheckResponseDto> fetchProductAndValidateStock(List<ProductStockCheckRequestDto> requestDtos) {
        // productId 추출
        List<Long> productIds = extractProductIdsFromCheckDto(requestDtos);
        // product 조회, 검증
        List<ProductStockDto> productStocks = getProductStocks(productIds);
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
        List<Long> productIds = requestDtos.stream().map(ProductStockCheckRequestDto::getProductId).collect(Collectors.toList());
        return productIds;
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
