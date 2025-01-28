package com.hong.orderservice.client.hotdeal;

import com.hong.common.dto.HotDealProductCommonDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "hotdeal-service")
public interface HotDealServiceClient {
    @PostMapping("/hotdeal-service/decrease-stock")
    List<HotDealProductCommonDto> fetchAndDecreaseStock(@RequestBody List<HotDealProductCommonDto> hotDealProductCommonDtos);

    @PostMapping("/hotdeal-service/increase-stock")
    List<HotDealProductCommonDto> fetchAndIncreaseStock(@RequestBody List<HotDealProductCommonDto> hotDealProductCommonDtos);
}
