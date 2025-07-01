package com.hong.hotdealservice.service;

import com.hong.hotdealservice.dto.HotDealPagingCacheDto;
import com.hong.hotdealservice.dto.HotDealCacheDto;
import com.hong.hotdealservice.dto.HotDealResponseDto;
import com.hong.hotdealservice.web.dto.HotDealCreateRequestDto;
import com.hong.hotdealservice.web.dto.HotDealUpdateRequestDto;

public interface HotDealService {

    // HotDeal 생성
    HotDealResponseDto createHotDeal(Long adminId, HotDealCreateRequestDto requestDto);

    // HotDeal 페이징 조회
    HotDealPagingCacheDto getHotDeals(String search, Long cursor, int size);

    // HotDeal 단건 조회
    HotDealCacheDto getHotDeal(Long hotDealId);

    // HotDeal 수정
    HotDealResponseDto updateHotDeal(Long hotDealId, HotDealUpdateRequestDto requestDto);

    // HotDeal 삭제
    HotDealResponseDto deleteHotDeal(Long hotDealId);
}
