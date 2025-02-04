package com.hong.productservice.service.product;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.ProductException;
import com.hong.productservice.domain.Product;
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

    //  products 조회
    public List<ProductCommonDto> getProductsByIds(List<Long> productIds) {
        List<Product> products = productRepository.findAllByProductIds(productIds);
        if (productIds.size() != products.size()) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return products.stream()
                .map(product -> new ProductCommonDto(
                        product.getId(),
                        product.getTitle(),
                        product.getPrice(),
                        product.getStock()))
                .collect(Collectors.toList()
                );
    }

    // products 조회
    public List<ProductCommonDto> fetchProducts(List<ProductCommonDto> requestDtos) {
        List<Long> collect = requestDtos.stream().map(ProductCommonDto::getId).collect(Collectors.toList());
        // product 조회, 검증
        List<Product> products = fetchAndValidate(collect);
        // dto 변환
        return convertDtos(requestDtos, products);
    }

    // product 재고 감소
    @Transactional
    public List<ProductStockUpdateResponseDto> decreaseStock(List<ProductStockUpdateRequestDto> requestDtos) {
        // productIds 추출
        List<Long> productIds = extractProductIds(requestDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(productIds);
        // proudcts 조회, 검증
        List<Product> products = fetchAndValidate(productIds);
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
        List<Long> productIds = extractProductIds(requestDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(productIds);
        // proudcts 조회, 검증
        List<Product> products = fetchAndValidate(productIds);
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

    // 조회한 products Dto 변환
    private List<ProductCommonDto> convertDtos(List<ProductCommonDto> productDtos, List<Product> products) {
        List<ProductCommonDto> responseDtos = new ArrayList<>();
        // map 변환
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        for (ProductCommonDto productDto : productDtos) {
            Product product = productMap.get(productDto.getId());
            responseDtos.add(new ProductCommonDto(product.getId(), product.getTitle(),
                    product.getPrice(), productDto.getQuantity(), productDto.getIsHotDealProduct()));
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
            log.info("productId = {}, 조회 재고 = {}", requestDto.getProductId(), originalStock);

            // product 재고 감소 (예외시 exception)
            product.decreaseStock(requestedStock);

            Integer remainingStock = product.getStock();
            log.info("productId = {}, 재고 감소 = {}, 반영 재고 = {}", requestDto.getProductId(), requestedStock, remainingStock);

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
            log.info("productId = {}, 조회 재고 = {}", requestDto.getProductId(), originalStock);

            // product 재고 증가
            product.increaseStock(requestedStock);

            Integer remainingStock = product.getStock();
            log.info("productId = {}, 재고 증가 = {}, 반영 재고 = {}", requestDto.getProductId(), requestedStock, remainingStock);

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

    // proudcts 조회, 검증
    private List<Product> fetchAndValidate(List<Long> productIds) {
        // product 조회
        List<Product> products = productRepository.findAllByProductIds(productIds);

        // 요청과, 조회된 product 와 다른 productIds 추출(디버깅, 예외 처리용)
        List<Long> noneMathProductIds = productIds.stream()
                .filter(id -> products.stream()
                        .noneMatch(product -> product.getId().equals(id)))
                .collect(Collectors.toList());

        if (products.size() != productIds.size()) {
            log.debug("요청된 상품이 존재하지 않습니다. productId = {}", noneMathProductIds);
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND, noneMathProductIds);
        }
        return products;
    }

    // productIds 추출
    private List<Long> extractProductIds(List<ProductStockUpdateRequestDto> requestDtos) {
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
}
