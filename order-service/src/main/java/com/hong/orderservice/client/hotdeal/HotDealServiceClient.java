package com.hong.orderservice.client.hotdeal;

import com.hong.common.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "hotdeal-service")
public interface HotDealServiceClient {

    @PostMapping("/hotdeal-service/products/stock/reserve")
    ProductReservationResponseDto reserveStock(@RequestBody ProductReservationRequestDto requestDto);

    @PostMapping("/hotdeal-service/products/stock/finalize")
    StockFinalizeResponseDto finalizeStockReservation(@RequestBody StockFinalizeRequestDto requestDto);

    @PostMapping("/hotdeal-service/products/stock/release")
    ReleaseReservedStockResponseDto releaseReservedStocks(@RequestBody ReleaseReservedStockRequestDto requestDto);

    @PostMapping("/hotdeal-service/products/stock/restore")
    StockRestoreResponseDto restoreStock(@RequestBody StockRestoreRequestDto requestDto);

}
