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
    public ResponseEntity<Boolean> decreaseStock(@RequestBody List<HotDealProductCommonDto> requestDtos) {
        Boolean result = hotDealApiService.decreaseStock(requestDtos);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/increase-stock")
    public ResponseEntity<Boolean> fetchAndIncreaseStock(@RequestBody List<HotDealProductCommonDto> requestDtos) {
        Boolean result = hotDealApiService.increaseStock(requestDtos);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/products")
    public ResponseEntity<List<HotDealProductCommonDto>> fetchProducts(@RequestBody List<HotDealProductCommonDto> requestDtos) {
        List<HotDealProductCommonDto> responseDtos = hotDealApiService.fetchProducts(requestDtos);
        return ResponseEntity.ok(responseDtos);
    }

}


