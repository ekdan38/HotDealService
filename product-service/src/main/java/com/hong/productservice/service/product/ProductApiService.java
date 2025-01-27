package com.hong.productservice.service.product;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealException;
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
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
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

    // product 재고 감소
    @Transactional
    public List<ProductCommonDto> decreaseStock(List<ProductCommonDto> productCommonDtos) {
        List<ProductCommonDto> responseDtos;
        // productIds 추출
        List<Long> productIds = extractProductIds(productCommonDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(productIds);
        // proudcts 조회, 검증
        List<Product> products = fetchAndValidate(productCommonDtos);
        // 재고 감소
        responseDtos = decreaseStock(productCommonDtos, products);
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
    public List<ProductCommonDto> increaseStock(List<ProductCommonDto> productCommonDtos) {
        List<ProductCommonDto> responseDtos;

        // productIds 추출
        List<Long> productIds = extractProductIds(productCommonDtos);
        // 락 획득
        List<RLock> locks = acquireLocks(productIds);
        // proudcts 조회, 검증
        List<Product> products = fetchAndValidate(productCommonDtos);
        // 재고 감소
        responseDtos = increaseStock(productCommonDtos, products);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                releaseLocks(locks);
            }
        });

        return responseDtos;
    }

    // 재고 감소
    private List<ProductCommonDto> decreaseStock(List<ProductCommonDto> productDtos, List<Product> products) {
        // map 변환
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        List<ProductCommonDto> responseDtos = new ArrayList<>();
        for (ProductCommonDto productDto : productDtos) {
            Product product = productMap.get(productDto.getId());
            log.info("productId = {}, 조회 재고 = {}", productDto.getId(), product.getStock());
            product.decreaseStock(productDto.getQuantity());
            log.info("productId = {}, 재고 감소 = {}", productDto.getId(), productDto.getQuantity());
            responseDtos.add(new ProductCommonDto(product.getId(), product.getTitle(),
                    product.getPrice(), null, productDto.getQuantity()));
        }
        return responseDtos;
    }

    // 재고 증가
    private List<ProductCommonDto> increaseStock(List<ProductCommonDto> productDtos, List<Product> products) {
        // map 변환
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        List<ProductCommonDto> responseDtos = new ArrayList<>();
        for (ProductCommonDto productDto : productDtos) {
            Product product = productMap.get(productDto.getId());
            log.info("productId = {}, 조회 재고 = {}", productDto.getId(), product.getStock());
            product.increaseStock(productDto.getQuantity());
            log.info("productId = {}, 재고 감소 = {}", productDto.getId(), productDto.getQuantity());
            responseDtos.add(new ProductCommonDto(product.getId(), product.getTitle(),
                    product.getPrice(), null, productDto.getQuantity()));
        }
        return responseDtos;
    }

    // proudcts 조회, 검증
    private List<Product> fetchAndValidate(List<ProductCommonDto> productDtos) {
        List<Long> productIds = productDtos.stream().map(ProductCommonDto::getId)
                .sorted()
                .collect(Collectors.toList());
        List<Product> products = productRepository.findAllByProductIds(productIds);
        if (products.size() != productIds.size()) {
            log.error("존재하지 않는 상품이 포함되어 있습니다.");
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return products;
    }

    // productIds 추출
    private List<Long> extractProductIds(List<ProductCommonDto> productCommonDtos) {
        return productCommonDtos.stream().map(ProductCommonDto::getId)
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
