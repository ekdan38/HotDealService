package com.hong.hotdealservice.service;

import com.hong.hotdealservice.dto.HotDealProductCacheDto;
import com.hong.hotdealservice.dto.HotDealProductPagingResponseDto;
import com.hong.hotdealservice.dto.HotDealProductResponseDto;

public interface HotDealProductService {

    // HotDealProduct 페이징 조회
    HotDealProductPagingResponseDto getHotDealProducts(Long hotDealId, String search, Long cursor, int size);

    // HotDealProduct 단건 조회
    HotDealProductCacheDto getHotDealProduct(Long hotDealProductId);
}
