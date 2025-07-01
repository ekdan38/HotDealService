package com.hong.hotdealservice.service;

import com.hong.hotdealservice.dto.ProductResponseDto;
import com.hong.hotdealservice.dto.ProductPagingResponseDto;

public interface HotDealProductService {

    // HotDealProduct 페이징 조회
    ProductPagingResponseDto getProducts(Long hotDealId, String search, Long cursor, int size);

    // HotDealProduct 단건 조회
    ProductResponseDto getProduct(Long hotDealProductId);
}
