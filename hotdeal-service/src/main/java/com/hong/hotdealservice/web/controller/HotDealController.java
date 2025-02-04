package com.hong.hotdealservice.web.controller;


import com.hong.common.dto.ResponseDto;
import com.hong.hotdealservice.dto.HotDealPagingResponseDto;
import com.hong.hotdealservice.dto.HotDealResponseDto;
import com.hong.hotdealservice.service.HotDealService;
import com.hong.hotdealservice.web.dto.HotDealRequestDto;
import com.hong.hotdealservice.web.dto.HotDealUpdateRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealController]")
@RequestMapping("/hotdeals")
public class HotDealController {

    private final HotDealService hotDealService;

    // HotDeal 생성
    // Admin
    @PostMapping
    public ResponseEntity<?> createHotDeal(@RequestHeader("X-User-Id") Long adminId,
                                           @RequestBody @Validated HotDealRequestDto requestDto,
                                           BindingResult bindingResult) {

        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("HotDeal 생성 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        HotDealResponseDto resultDto = hotDealService.createHotDeal(adminId, requestDto);

        // 응답 설정
        ResponseDto<HotDealResponseDto> responseDto = new ResponseDto<>("HotDeal 생성 성공", resultDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    // HotDeal 페이징 조회
    // 간단하게 HotDeal 내역 조회
    @GetMapping
    public ResponseEntity<ResponseDto<HotDealPagingResponseDto>> getHotDeals(@RequestParam(required = false) Long cursor,
                                                                             @RequestParam(required = false, defaultValue = "10") int size,
                                                                             @RequestParam(required = false) String search) {

        HotDealPagingResponseDto resultDto = hotDealService.getHotDeals(search, cursor, size);

        // 응답 설정
        ResponseDto<HotDealPagingResponseDto> responseDto = new ResponseDto<>("HotDeal 조회 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // HotDeal 단건 조회
    @GetMapping("/{hotDealId}")
    public ResponseEntity<ResponseDto<HotDealResponseDto>> getHotDeal(@PathVariable("hotDealId") Long hotDealId) {

        HotDealResponseDto resultDto = hotDealService.getHotDeal(hotDealId);

        // 응답 설정
        ResponseDto<HotDealResponseDto> responseDto = new ResponseDto<>("HotDeal 조회 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // HotDeal 수정
    // Admin
    @PutMapping("/{hotDealId}")
    public ResponseEntity<?> updateHotDeal(@PathVariable("hotDealId") Long hotDealId,
                                           @RequestBody @Validated HotDealUpdateRequestDto requestDto,
                                           BindingResult bindingResult) {
        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("HotDeal 수정 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        HotDealResponseDto resultDto = hotDealService.updateHotDeal(hotDealId, requestDto);
        // 응답 설정
        ResponseDto<HotDealResponseDto> responseDto = new ResponseDto<>("HotDeal 수정 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);

    }

    // HotDeal 삭제
    // Admin
    @DeleteMapping("/{hotDealId}")
    public ResponseEntity<ResponseDto<HotDealResponseDto>> deleteHotDeal(@PathVariable("hotDealId") Long hotDealId) {

        HotDealResponseDto resultDto = hotDealService.deleteHotDeal(hotDealId);
        // 응답 설정
        ResponseDto<HotDealResponseDto> responseDto = new ResponseDto<>("HotDeal 삭제 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

}
