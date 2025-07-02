package com.hong.paymentservice.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.common.exception.custom.PaymentException;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
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

        if (methodKey.contains("OrderServiceClient#fetchOrder")){
            if(status == 503){
                return FeignException.errorStatus(methodKey, response);
            }
            else if(status == 404){
                log.error("OrderServiceClient#fetchOrder. 404 응답. ErrorMessage = {}" ,errorMessage);
                throw new PaymentException(ErrorCode.PAYMENT_FETCH_ORDER_NOT_FOUND, errorMessage);
            }
        }

        else if (methodKey.contains("OrderServiceClient#updateOrderStatus")){
            if(status == 503){
                return FeignException.errorStatus(methodKey, response);
            }
            else if(status == 404){
                log.error("OrderServiceClient#updateOrderStatus. 404 응답. ErrorMessage = {}" ,errorMessage);
                throw new PaymentException(ErrorCode.PAYMENT_UPDATE_ORDER_NOT_FOUND, errorMessage);
            }
        }

        return FeignException.errorStatus(methodKey, response, null, null);
    }
}
