package com.hong.hotdealservice.web.controller;

import com.hong.common.dto.HotDealProductCommonDto;
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
    public ResponseEntity<List<HotDealProductCommonDto>> fetchAndDecreaseStock(@RequestBody List<HotDealProductCommonDto> hotDealProductCommonDtos) {
        List<HotDealProductCommonDto> responseDtos = hotDealApiService.fetchAndDecreaseStock(hotDealProductCommonDtos);
        return ResponseEntity.ok(responseDtos);
    }

    @PostMapping("/increase-stock")
    public ResponseEntity<List<HotDealProductCommonDto>> fetchAndIncreaseStock(@RequestBody List<HotDealProductCommonDto> hotDealProductCommonDtos) {
        List<HotDealProductCommonDto> responseDtos = hotDealApiService.fetchAndIncreaseStock(hotDealProductCommonDtos);
        return ResponseEntity.ok(responseDtos);
    }
}


