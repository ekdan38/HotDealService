package com.hong.hotdealservice.web.controller;

import com.hong.common.dto.ResponseDto;
import com.hong.hotdealservice.dto.HotDealProductCacheDto;
import com.hong.hotdealservice.dto.HotDealProductPagingResponseDto;
import com.hong.hotdealservice.dto.HotDealProductStockDto;
import com.hong.hotdealservice.service.HotDealApiService;
import com.hong.hotdealservice.service.HotDealProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealProductController]")
@RequestMapping("/hotdeals")
public class HotDealProductController {

    private final HotDealProductService hotDealProductService;
    private final HotDealApiService hotDealApiService;

    // HotDealProduct 페이징 조회
    @GetMapping("/{hotDealId}/hotDealProducts")
    public ResponseEntity<ResponseDto<HotDealProductPagingResponseDto>> getHotDealProducts(@PathVariable("hotDealId") Long hotDealId,
                                                                                           @RequestParam(required = false) Long cursor,
                                                                                           @RequestParam(required = false, defaultValue = "10") Integer size,
                                                                                           @RequestParam(required = false) String search) {

        HotDealProductPagingResponseDto resultDto = hotDealProductService.getHotDealProducts(hotDealId, search, cursor, size);

        // 응답 설정
        ResponseDto<HotDealProductPagingResponseDto> responseDto = new ResponseDto<>("HotDealProducts 조회 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // HotDealProduct 단건 조회
    @GetMapping("/hotDealProducts/{hotDealProductId}")
    public ResponseEntity<ResponseDto<HotDealProductCacheDto>> getHotDealProduct(@PathVariable("hotDealProductId") Long hotDealProductId) {

        HotDealProductCacheDto resultDto = hotDealProductService.getHotDealProduct(hotDealProductId);

        // 응답 설정
        ResponseDto<HotDealProductCacheDto> responseDto = new ResponseDto<>("HotDealProduct 조회 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    @GetMapping("/hotDealProducts/stock")
    public ResponseEntity<ResponseDto<List<HotDealProductStockDto>>> getHotDealProductStock(@RequestParam List<Long> hotDealProductId){
        List<HotDealProductStockDto> resultDto = hotDealApiService.getHotDealProductsWithStock(hotDealProductId);

        // 응답 설정
        ResponseDto<List<HotDealProductStockDto>> responseDto = new ResponseDto<>("HotDealProducts 재고 조회 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }
}
