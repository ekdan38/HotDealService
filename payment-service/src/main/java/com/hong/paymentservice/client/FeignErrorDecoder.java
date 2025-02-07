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
        String errorMessage;
        try {
            String responseBody = new String(response.body().asInputStream().readAllBytes());
            Map<String, String> errorMap = objectMapper.readValue(responseBody,
                    new TypeReference<>() {
                    });
            errorMessage = errorMap.get("errorMessage");

        } catch (IOException e) {
            log.debug("feign Client 에러 응답 파싱 실패 했습니다. errorMessage = {}", e.getMessage());
            throw new PaymentException(ErrorCode.PAYMENT_PARSE_RESPONSE_FAILED);
        }

        if (methodKey.contains("HotDealServiceClient#decreaseStock") ||
                methodKey.contains("HotDealServiceClient#increaseStock")){
            throw new PaymentException(ErrorCode.PAYMENT_ORDER_SERVICE_FAILED, errorMessage);
        }
        else if (methodKey.contains("ProductServiceClient#decreaseStock") ||
                methodKey.contains("ProductServiceClient#increaseStock")) {
            throw new PaymentException(ErrorCode.PAYMENT_ORDER_SERVICE_FAILED, errorMessage);
        }
        else if (methodKey.contains("OrderServiceClient#fetchOrder") ||
                methodKey.contains("OrderServiceClient#updateOrderStatus")) {
            throw new PaymentException(ErrorCode.PAYMENT_ORDER_SERVICE_FAILED, errorMessage);
        }
        else{
            return FeignException.errorStatus(methodKey, response, null, null);
        }
    }
}
