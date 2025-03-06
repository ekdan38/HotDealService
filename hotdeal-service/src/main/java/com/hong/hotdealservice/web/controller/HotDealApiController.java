package com.hong.hotdealservice.web.controller;

import com.hong.common.dto.*;
import com.hong.hotdealservice.service.HotDealApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealApiController]")
@RequestMapping("/hotdeal-service")
public class HotDealApiController {

    private final HotDealApiService hotDealApiService;

    @PostMapping("/products")
    public ResponseEntity<List<HotDealProductStockCheckResponseDto>> fetchProducts(@RequestBody List<HotDealProductStockCheckRequestDto> requestDtos) {
        List<HotDealProductStockCheckResponseDto> responseDtos = hotDealApiService.fetchHotDealProductsStockAndValidateStock(requestDtos);
        return ResponseEntity.ok(responseDtos);
    }
    @PostMapping("/decrease-stock")
    public ResponseEntity<List<HotDealProductStockUpdateResponseDto>> decreaseStock(@RequestBody List<HotDealProductStockUpdateRequestDto> requestDtos) {
       List<HotDealProductStockUpdateResponseDto> responseDtos = hotDealApiService.decreaseStock(requestDtos);
        return ResponseEntity.ok(responseDtos);
    }

    @PostMapping("/increase-stock")
    public ResponseEntity<List<HotDealProductStockUpdateResponseDto>> increaseStock(@RequestBody List<HotDealProductStockUpdateRequestDto> requestDtos) {
        List<HotDealProductStockUpdateResponseDto> responseDtos = hotDealApiService.increaseStock(requestDtos);
        return ResponseEntity.ok(responseDtos);
    }



}


