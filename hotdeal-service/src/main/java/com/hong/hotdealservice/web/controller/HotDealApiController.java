package com.hong.hotdealservice.web.controller;

import com.hong.common.dto.*;
import com.hong.hotdealservice.service.HotDealProductStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealApiController]")
@RequestMapping("/hotdeal-service")
public class HotDealApiController {

    private final HotDealProductStockService hotDealProductStockService;

    @PostMapping("/products/stock/reserve")
    public ResponseEntity<ProductReservationResponseDto> reserveProducts(@RequestBody ProductReservationRequestDto requestDto) {
        ProductReservationResponseDto responseDto = hotDealProductStockService.reserveStock(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/products/stock/finalize")
    public ResponseEntity<StockFinalizeResponseDto> finalizeStockReservation(@RequestBody StockFinalizeRequestDto requestDto) {
        StockFinalizeResponseDto responseDto = hotDealProductStockService.handleStockFinalization(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/products/stock/release")
    public ResponseEntity<ReleaseReservedStockResponseDto> releaseReservedStocks(@RequestBody ReleaseReservedStockRequestDto requestDto){
        ReleaseReservedStockResponseDto responseDto = hotDealProductStockService.releaseReservedStocks(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/products/stock/restore")
    public ResponseEntity<StockRestoreResponseDto> restoreStock(@RequestBody StockRestoreRequestDto requestDto){
        StockRestoreResponseDto responseDto = hotDealProductStockService.restoreStock(requestDto);
        return ResponseEntity.ok(responseDto);
    }
}


