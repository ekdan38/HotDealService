package com.hong.hotdealservice.service;

import com.hong.hotdealservice.dto.HotDealPagingCacheDto;
import com.hong.hotdealservice.dto.HotDealCacheDto;
import com.hong.hotdealservice.web.dto.HotDealRequestDto;
import com.hong.hotdealservice.web.dto.HotDealUpdateRequestDto;

public interface HotDealService {

    // HotDeal 생성
    HotDealCacheDto createHotDeal(Long adminId, HotDealRequestDto requestDto);

    // HotDeal 페이징 조회
    HotDealPagingCacheDto getHotDeals(String search, Long cursor, int size);

    // HotDeal 단건 조회
    HotDealCacheDto getHotDeal(Long hotDealId);

    // HotDeal 수정
    HotDealCacheDto updateHotDeal(Long hotDealId, HotDealUpdateRequestDto requestDto);

    // HotDeal 삭제
    HotDealCacheDto deleteHotDeal(Long hotDealId);
}
