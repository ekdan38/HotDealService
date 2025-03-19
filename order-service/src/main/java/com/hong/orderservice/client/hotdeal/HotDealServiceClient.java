package com.hong.orderservice.client.hotdeal;

import com.hong.common.dto.HotDealProductStockCheckRequestDto;
import com.hong.common.dto.HotDealProductStockCheckResponseDto;
import com.hong.common.dto.HotDealProductStockUpdateRequestDto;
import com.hong.common.dto.HotDealProductStockUpdateResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "hotdeal-service")
public interface HotDealServiceClient {

    @PostMapping("/hotdeal-service/products")
    List<HotDealProductStockCheckResponseDto> fetchProductsAndValidateStock(@RequestBody List<HotDealProductStockCheckRequestDto> requestDtos);

    @PostMapping("/hotdeal-service/decrease-stock")
    List<HotDealProductStockUpdateResponseDto> decreaseStock(@RequestBody List<HotDealProductStockUpdateRequestDto> requestDtos);

    @PostMapping("/hotdeal-service/increase-stock")
    List<HotDealProductStockUpdateResponseDto> increaseStock(@RequestBody List<HotDealProductStockUpdateRequestDto> requestDtos);
}
