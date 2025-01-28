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
            return new OrderException(ErrorCode.ORDER_PRODUCT_PARSE_RESPONSE_FAILED);
        }

        switch (response.status()) {
            case 400:
                if (methodKey.contains("HotDealServiceClient#fetchAndDecreaseStock")) {
                    return new OrderException(ErrorCode.ORDER_HOTDEAL_PRODUCT_SERVICE_FAILED, errorMessage);
                } else if (methodKey.contains("ProductServiceClient#fetchAndDecreaseStock")) {
                    return new OrderException(ErrorCode.ORDER_PRODUCT_SERVICE_FAILED, errorMessage);
                }
                return FeignException.errorStatus(methodKey, response, null, null);

            case 404:
                if (methodKey.contains("HotDealServiceClient#fetchAndDecreaseStock")) {
                    return new OrderException(ErrorCode.ORDER_HOTDEAL_PRODUCT_SERVICE_FAILED, errorMessage);
                }else if (methodKey.contains("ProductServiceClient#fetchAndDecreaseStock")) {
                    return new OrderException(ErrorCode.ORDER_PRODUCT_SERVICE_FAILED, errorMessage);
                }
                return FeignException.errorStatus(methodKey, response, null, null);
            case 409:
                if (methodKey.contains("HotDealServiceClient#fetchAndDecreaseStock")) {
                    throw new OrderException(ErrorCode.ORDER_HOTDEAL_PRODUCT_SERVICE_FAILED, errorMessage);
                }else if (methodKey.contains("ProductServiceClient#fetchAndDecreaseStock")) {
                    return new OrderException(ErrorCode.ORDER_PRODUCT_SERVICE_FAILED, errorMessage);
                }
                return FeignException.errorStatus(methodKey, response, null, null);
            case 503:
                if (methodKey.contains("HotDealServiceClient#fetchAndDecreaseStock")) {
                    throw new OrderException(ErrorCode.ORDER_HOTDEAL_PRODUCT_SERVICE_FAILED, errorMessage);
                }
                return FeignException.errorStatus(methodKey, response, null, null);
            default:
                return FeignException.errorStatus(methodKey, response, null, null);
        }
    }
}
