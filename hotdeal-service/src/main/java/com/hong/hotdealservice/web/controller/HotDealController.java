package com.hong.hotdealservice.web.controller;


import com.hong.common.dto.ResponseDto;
import com.hong.hotdealservice.dto.HotDealPagingCacheDto;
import com.hong.hotdealservice.dto.HotDealCacheDto;
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

        HotDealCacheDto resultDto = hotDealService.createHotDeal(adminId, requestDto);

        // 응답 설정
        ResponseDto<HotDealCacheDto> responseDto = new ResponseDto<>("핫딜 생성 성공", resultDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    // HotDeal 페이징 조회
    @GetMapping
    public ResponseEntity<ResponseDto<HotDealPagingCacheDto>> getHotDeals(@RequestParam(required = false) Long cursor,
                                                                          @RequestParam(required = false, defaultValue = "10") int size,
                                                                          @RequestParam(required = false) String search) {

        HotDealPagingCacheDto resultDto = hotDealService.getHotDeals(search, cursor, size);

        // 응답 설정
        ResponseDto<HotDealPagingCacheDto> responseDto = new ResponseDto<>("핫딜 페이징 조회 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // HotDeal 단건 조회
    @GetMapping("/{hotDealId}")
    public ResponseEntity<ResponseDto<HotDealCacheDto>> getHotDeal(@PathVariable("hotDealId") Long hotDealId) {

        HotDealCacheDto resultDto = hotDealService.getHotDeal(hotDealId);

        // 응답 설정
        ResponseDto<HotDealCacheDto> responseDto = new ResponseDto<>("핫딜 단건 조회 성공", resultDto);
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

        HotDealCacheDto resultDto = hotDealService.updateHotDeal(hotDealId, requestDto);
        // 응답 설정
        ResponseDto<HotDealCacheDto> responseDto = new ResponseDto<>("핫딜 수정 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);

    }

    // HotDeal 삭제
    // Admin
    @DeleteMapping("/{hotDealId}")
    public ResponseEntity<ResponseDto<HotDealCacheDto>> deleteHotDeal(@PathVariable("hotDealId") Long hotDealId) {

        HotDealCacheDto resultDto = hotDealService.deleteHotDeal(hotDealId);
        // 응답 설정
        ResponseDto<HotDealCacheDto> responseDto = new ResponseDto<>("핫딜 삭제 성공", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

}
