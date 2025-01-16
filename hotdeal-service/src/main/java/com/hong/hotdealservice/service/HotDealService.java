package com.hong.hotdealservice.service;

import com.hong.hotdealservice.dto.HotDealPagingResponseDto;
import com.hong.hotdealservice.dto.HotDealResponseDto;
import com.hong.hotdealservice.web.dto.HotDealRequestDto;
import com.hong.hotdealservice.web.dto.HotDealUpdateRequestDto;

public interface HotDealService {

    // HotDeal 생성
    // Admin
    public HotDealResponseDto createHotDeal(Long adminId, HotDealRequestDto requestDto);

    // HotDeal 페이징 조회
    // 간단한게 HotDeal 내역 조회
    public HotDealPagingResponseDto getHotDeals(String search, Long cursor, int size);

    // HotDeal 단건 조회
    public HotDealResponseDto getHotDeal(Long hotDealId);

    // HotDeal 수정
    // Admin
    public HotDealResponseDto updateHotDeal(Long hotDealId, HotDealUpdateRequestDto requestDto);

    // HotDeal 삭제
    // Admin
    public HotDealResponseDto deleteHotDeal(Long hotDealId);
}
