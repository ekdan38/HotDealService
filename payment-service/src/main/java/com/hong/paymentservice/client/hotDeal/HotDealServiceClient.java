package com.hong.paymentservice.client.hotDeal;

import com.hong.common.dto.HotDealProductStockUpdateRequestDto;
import com.hong.common.dto.HotDealProductStockUpdateResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "hotdeal-service")
public interface HotDealServiceClient {

    @PostMapping("/hotdeal-service/decrease-stock")
    List<HotDealProductStockUpdateResponseDto> decreaseStock(@RequestBody List<HotDealProductStockUpdateRequestDto> requestDtos);

    @PostMapping("/hotdeal-service/increase-stock")
    List<HotDealProductStockUpdateResponseDto> increaseStock(@RequestBody List<HotDealProductStockUpdateRequestDto> requestDtos);
}

