package com.hong.orderservice.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j(topic = "FeignErrorDecoder")
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String errorMessage = "null";
        try {
            if (response.body() != null) {
                byte[] bodyBytes = response.body().asInputStream().readAllBytes();
                String responseBody = new String(bodyBytes);
                Map<String, String> errorMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });
                errorMessage = errorMap.getOrDefault("errorMessage", "Unknown ErrorMessage");
            } else {
                // body가 없는 경우
                errorMessage = String.format("Feign 호출 실패: 응답 body 없음 (methodKey: %s, status: %d)", methodKey, response.status());
            }
        } catch (IOException e) {
            log.info("Feign Client 응답 파싱 실패");
        }

        int status = response.status();
        /**
         * userService
         */
        if (methodKey.contains("UserServiceClient#deleteUserCart")){
            if (status == 503) {
                return FeignException.errorStatus(methodKey, response);
            }
            else{
                throw new OrderException(ErrorCode.ORDER_USER_SERVICE_FAILED, errorMessage);
            }
        }

        /**
         * hotdealService
         */
        // 재고 점유 요청
        else if (methodKey.contains("HotDealServiceClient#reserveStock")){
            if (status == 503) {
                log.error("HotDealServiceClient#reserveStock. 503 응답. StatusCode = {}, ErrorMessage = {}" ,status ,errorMessage);
                return FeignException.errorStatus(methodKey, response);
            }
            else if(status == 400 || status == 404){
                log.error("HotDealServiceClient#reserveStock. {} 응답. ErrorMessage = {}" ,status ,errorMessage);
                throw new OrderException(ErrorCode.ORDER_HOTDEAL_SERVICE_FAILED, errorMessage);
            }
        }

        // 재고 최종 반영 요청
        else if(methodKey.contains("HotDealServiceClient#finalizeStockReservation")){
            if(status == 503){
                return FeignException.errorStatus(methodKey, response);
            }
            else if(status == 400 || status == 404){
                throw new OrderException(ErrorCode.ORDER_HOTDEAL_SERVICE_FAILED, errorMessage);
            }
        }

        // 재고 점유 해제 요청
        else if(methodKey.contains("HotDealServiceClient#releaseReservedStocks")){
            if(status == 503){
                return FeignException.errorStatus(methodKey, response);
            }
            else if(status == 400 || status == 404){
                throw new OrderException(ErrorCode.ORDER_HOTDEAL_SERVICE_FAILED, errorMessage);
            }
        }

        /**
         * paymentService
         */
        // 주문 생성시 결제 생성 요청
        else if (methodKey.contains("PaymentServiceClient#createPayment")) {
            if (status == 503) {
                return FeignException.errorStatus(methodKey, response);
            }
            else if(status == 400 || status == 404){
                throw new OrderException(ErrorCode.ORDER_PAYMENT_SERVICE_FAILED, errorMessage);
            }
        }

        // 미결제 주문 만료 처리 => 결제 만료 처리 요청
        else if(methodKey.contains("PaymentServiceClient#expirePayment")){
            if(status == 503){
                return FeignException.errorStatus(methodKey, response);
            }
            else if(status == 400 || status == 404){
                throw new OrderException(ErrorCode.ORDER_PAYMENT_SERVICE_FAILED, errorMessage);
            }
        }

        else if(methodKey.contains("PaymentServiceClient#cancelPayment")){
            if(status == 503){
                return FeignException.errorStatus(methodKey, response);
            }
            else if(status == 400 || status == 404){
                throw new OrderException(ErrorCode.ORDER_PAYMENT_SERVICE_FAILED, errorMessage);
            }
        }
        return FeignException.errorStatus(methodKey, response);
    }
}
