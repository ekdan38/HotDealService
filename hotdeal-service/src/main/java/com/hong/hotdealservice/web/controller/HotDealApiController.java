package com.hong.hotdealservice.web.controller;

import com.hong.common.dto.HotDealProductDto;
import com.hong.common.dto.ProductStockDto;
import com.hong.hotdealservice.service.HotDealApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealApiController]")
@RequestMapping("/hotdeal-service")
public class HotDealApiController {

    private final HotDealApiService hotDealApiService;

    @PostMapping("/decrease-stock")
    public ResponseEntity<Boolean> fetchAndDecreaseStock(@RequestBody List<HotDealProductDto> hotDealProductDtos) {
        Boolean result = hotDealApiService.fetchAndDecreaseStock(hotDealProductDtos);
        return ResponseEntity.ok(result);
    }
}


