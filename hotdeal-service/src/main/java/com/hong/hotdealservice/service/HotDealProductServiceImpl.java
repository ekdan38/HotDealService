package com.hong.hotdealservice.service;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealProductException;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.HotDealProductCacheDto;
import com.hong.hotdealservice.dto.HotDealProductPagingResponseDto;
import com.hong.hotdealservice.dto.HotDealProductResponseDto;
import com.hong.hotdealservice.repository.HotDealProductRepository;
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


    // HotDealProduct 페이징 조회
    @Override
    @Cacheable(cacheNames = "getHotDealProducts"
            , key = "'hotdeal:' + #hotDealId + 'hotdeal_products:cursor:' +" +
            " (#cursor == null ? '' : #cursor) + ':size:' + #size + ':search:' + (#search == null ? '' : #search)"
            , cacheManager = "HotDealCacheManager")
    public HotDealProductPagingResponseDto getHotDealProducts(Long hotDealId, String search, Long cursor, int size) {
        // 1. hotDealProducts 커서 기반 페이징 조회
        List<HotDealProduct> page = fetchHotDealProductsByCursor(hotDealId, search, cursor, size);

        // 2. cursor 지정 및 응답 Dto 변환
        return convertToHotDealProductPagingResponse(hotDealId, page);
    }


    // HotDealProduct 단건 조회
    @Override
    @Cacheable(cacheNames = "getHotDealProduct"
            , key = "'hotdeal_products:' + #hotDealProductId", cacheManager = "HotDealCacheManager")
    public HotDealProductCacheDto getHotDealProduct(Long hotDealProductId) {
        // 1. hotDealProduct 조회 및 검증
        HotDealProduct hotDealProduct = fetchByIdAndValidate(hotDealProductId);

        // 2. 응답 dto 변환
        return convertHotDealCacheDtoWithoutStock(hotDealProduct);
    }

    private HotDealProductPagingResponseDto convertToHotDealProductPagingResponse(Long hotDealId, List<HotDealProduct> page) {
        // nextCursor 지정
        Long nextCursor = page.isEmpty() ? 0 : page.get(page.size() - 1).getId();

        // 응답 dto 변환
        return convertToHotDealPagingResponseDto(hotDealId, nextCursor, page);
    }

    private List<HotDealProduct> fetchHotDealProductsByCursor(Long hotDealId, String search, Long cursor, int size) {
        // cursor 가 null 이면 가장 최근 데이터 조회 처리
        if (cursor == null) cursor = Long.MAX_VALUE;

        // PageRequest 객체로 조회 size 지정
        PageRequest pageRequest = PageRequest.of(0, size);

        // 페이징 조회
        List<HotDealProduct> page = hotDealProductRepository.findByCursorAndSearchAndSizeHotDealProducts(hotDealId, cursor, search, pageRequest);
        return page;
    }

    // hotDealProduct 단건 조회, 검증
    private HotDealProduct fetchByIdAndValidate(Long hotDealProductId) {
        return hotDealProductRepository.findById(hotDealProductId).orElseThrow(() -> {
            log.debug("요청된 핫딜 상품이 존재하지 않습니다. hotDealProductId = {}", hotDealProductId);
            return new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, hotDealProductId);
        });
    }

    // hotDealProductResponseDto 변환
    private HotDealProductCacheDto convertHotDealCacheDtoWithoutStock(HotDealProduct hotDealProduct) {
        return new HotDealProductCacheDto(hotDealProduct);
    }

    // hotDealProductResponseDto 변환
    private HotDealProductPagingResponseDto convertToHotDealPagingResponseDto(Long hotDealId, Long nextCursor, List<HotDealProduct> page) {
        return new HotDealProductPagingResponseDto(
                nextCursor,
                hotDealId,
                page.stream()
                        .map(HotDealProductResponseDto::new)
                        .collect(Collectors.toList()));
    }
}
