package com.hong.hotdealservice.service;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealProductException;
import com.hong.hotdealservice.dto.ProductCacheDto;
import com.hong.hotdealservice.dto.ProductResponseDto;
import com.hong.hotdealservice.dto.ProductPagingResponseDto;
import com.hong.hotdealservice.dto.projection.ProductSimpleDto;
import com.hong.hotdealservice.repository.HotDealProductRepository;
import com.hong.hotdealservice.repository.ProductRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealProductServiceImpl]")
@Transactional(readOnly = true)
public class HotDealProductServiceImpl implements HotDealProductService {

    private final HotDealProductRepository hotDealProductRepository;
    private final ProductRedisRepository productRedisRepository;

    // HotDealProduct 페이징 조회
    @Override
    @Cacheable(cacheNames = "getProducts"
            , key = "'hotdeal:' + #hotDealId + ':products:cursor:' +" +
            " (#cursor == null ? '' : #cursor) + ':size:' + #size + ':search:' + (#search == null ? '' : #search)"
            , cacheManager = "HotDealCacheManager")
    public ProductPagingResponseDto getProducts(Long hotDealId, String search, Long cursor, int size) {
        // 1. hotDealProducts 커서 기반 페이징 조회
        List<ProductSimpleDto> page = getProductsByCursor(hotDealId, search, cursor, size);

        // 2. cursor 지정 및 응답 Dto 변환
        return convertToProductPagingResponse(hotDealId, page);
    }

    // HotDealProduct 단건 조회
    @Override
    public ProductResponseDto getProduct(Long productId) {
        // 1. hotDealProduct 조회 및 검증
        ProductSimpleDto product = getProductAndValidate(productId);

        // 2. 응답 dto 변환
        return convertProductCacheDtoWithoutStock(product);
    }

    private ProductPagingResponseDto convertToProductPagingResponse(Long hotDealId, List<ProductSimpleDto> page) {
        // nextCursor 지정
        Long nextCursor = page.isEmpty() ? 0 : page.get(page.size() - 1).getId();

        // 응답 dto 변환
        return convertToHotDealPagingResponseDto(hotDealId, nextCursor, page);
    }

    private List<ProductSimpleDto> getProductsByCursor(Long hotDealId, String search, Long cursor, int size) {
        // cursor 가 null 이면 가장 최근 데이터 조회 처리
        if (cursor == null) cursor = Long.MAX_VALUE;

        // PageRequest 객체로 조회 size 지정
        PageRequest pageRequest = PageRequest.of(0, size);

        // 페이징 조회
        return hotDealProductRepository
                .findByCursorAndSearchAndSizeHotDealProducts(hotDealId, cursor, search, pageRequest);
    }

    //hotDealProduct 단건 조회, 검증
    private ProductSimpleDto getProductAndValidate(Long productId) {

        // 1. Redis 캐시 조회
        ProductCacheDto cachedData = productRedisRepository.getProductById(productId);
        if(cachedData != null){
            return new ProductSimpleDto(cachedData);
        }

        // 2. CacheMiss -> DB 조회
        ProductSimpleDto product = hotDealProductRepository.findActiveProductById(productId).orElseThrow(() -> {
            log.debug("요청된 핫딜 상품이 존재하지 않습니다. hotDealProductId = {}", productId);
            return new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, productId);
        });

        // 3. Redis 에 캐시 저장
        ProductCacheDto cacheDto = new ProductCacheDto(product);
        productRedisRepository.saveWithTTL(cacheDto);

        return product;
    }

    // hotDealProductResponseDto 변환
    private ProductResponseDto convertProductCacheDtoWithoutStock(ProductSimpleDto product) {
        return new ProductResponseDto(product, product.getHotDealId());
    }

    // hotDealProductResponseDto 변환
    private ProductPagingResponseDto convertToHotDealPagingResponseDto(Long hotDealId, Long nextCursor, List<ProductSimpleDto> page) {
        return new ProductPagingResponseDto(
                nextCursor,
                hotDealId,
                page.stream()
                        .map(ProductResponseDto::new)
                        .collect(Collectors.toList()));
    }
}